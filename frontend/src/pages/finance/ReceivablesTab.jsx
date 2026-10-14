import { useCallback, useEffect, useState } from 'react';
import { Download, HandCoins } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { creditService } from '../../services/salesService';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/tables/DataTable';
import { formatDateTime, formatDT } from '../../utils/format';
import { PAYMENT_MODES } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';
import { exportCsv } from '../../utils/exportCsv';

const days = (iso) => Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);

function SettleModal({ client, onClose, onDone }) {
  const [sales, setSales] = useState(null);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState({ mode: 'ESPECES', montant: '', reference: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { creditService.openCredits(client.clientId).then(setSales).catch((e) => setError(e.message)); }, [client]);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      await creditService.settle(selected.id, { mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null });
      onDone();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  return (
    <Modal title={`Encaisser un règlement : ${client.nom}`} onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Fermer</Button>
             <Button onClick={submit} loading={busy} disabled={!selected || !toMillimes(form.montant)}>Encaisser</Button></>)}>
      <Alert type="error">{error}</Alert>
      {!sales ? <Spinner /> : (
        <>
          <p className="muted" style={{ marginBottom: 8 }}>Choisissez la vente réglée (la plus ancienne en premier).</p>
          <table className="table"><tbody>{sales.map((s) => (
            <tr key={s.id} className="clickable" onClick={() => { setSelected(s); setForm((f) => ({ ...f, montant: String(s.resteAPayer) })); }}
                style={selected?.id === s.id ? { background: 'var(--olive-100)' } : undefined}>
              <td><input type="radio" readOnly checked={selected?.id === s.id} /></td>
              <td><strong>{s.numeroTicket}</strong></td><td>{formatDateTime(s.date)}</td>
              <td className="num">{formatDT(s.total)}</td><td className="num"><strong>{formatDT(s.resteAPayer)}</strong></td>
            </tr>
          ))}</tbody></table>
          {selected ? (
            <div className="form-grid" style={{ marginTop: 14 }}>
              <div className="field"><label>Mode</label>
                <select className="select" value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
                  {Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select></div>
              <div className="field"><label>Montant (DT)</label>
                <input className="input" inputMode="decimal" value={form.montant} onChange={(e) => setForm({ ...form, montant: e.target.value })} /></div>
              <div className="field wide"><label>Référence</label>
                <input className="input" value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} /></div>
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}

export default function ReceivablesTab() {
  const { can } = useAuth();
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [settling, setSettling] = useState(null);
  const load = useCallback(() => creditService.receivables().then(setRows).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);
  if (!rows) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  const total = rows.reduce((s, r) => s + toMillimes(r.reste), 0) / 1000;
  return (
    <>
      <div className="stats">
        <StatCard label="Total dû par les clients" value={formatDT(total)} tone={total ? 'warn' : undefined} />
        <StatCard label="Clients concernés" value={rows.length} />
      </div>
      <div className="panel">
        <div className="section-title"><h2>Créances clients (ventes à crédit)</h2>
          <Button size="sm" variant="secondary" onClick={() => exportCsv('creances-clients', [
            { header: 'Code', value: (r) => r.code }, { header: 'Client', value: (r) => r.nom }, { header: 'Téléphone', value: (r) => r.telephone },
            { header: 'Ventes', value: (r) => r.nbVentes }, { header: 'Reste dû', value: (r) => Number(r.reste) },
            { header: 'Plus ancienne', value: (r) => formatDateTime(r.plusAncienne) },
          ], rows)}><Download size={16} />Exporter</Button></div>
        <DataTable rows={rows} rowKey="clientId" empty={<EmptyState icon={HandCoins} title="Aucune créance" text="Tous les clients sont à jour." />}
                   columns={[
                     { key: 'n', header: 'Client', render: (r) => <><strong>{r.nom}</strong><div className="muted" style={{ fontSize: 12 }}>{r.code}{r.telephone ? ` · ${r.telephone}` : ''}</div></> },
                     { key: 'v', header: 'Ventes', align: 'right', render: (r) => r.nbVentes },
                     { key: 'a', header: 'Ancienneté', render: (r) => { const d = days(r.plusAncienne); return <span className={`badge ${d > 60 ? 'badge-off' : d > 30 ? 'badge-warn' : ''}`}>{d} j</span>; } },
                     { key: 'r', header: 'Reste dû', align: 'right', render: (r) => <strong>{formatDT(r.reste)}</strong> },
                     ...(can(['SALES_CREATE', 'FINANCE_MANAGE']) ? [{ key: 'x', header: '', align: 'right', render: (r) => <Button size="sm" onClick={() => setSettling(r)}>Encaisser</Button> }] : []),
                   ]} />
      </div>
      {settling ? <SettleModal client={settling} onClose={() => setSettling(null)} onDone={() => { setSettling(null); load(); }} /> : null}
    </>
  );
}
