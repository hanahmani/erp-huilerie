import { useEffect, useState } from 'react';
import { Ban, CheckCircle2, PackageCheck, Pencil, Wallet } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Tabs from '../../components/common/Tabs';
import { useAuth } from '../../context/AuthContext';
import { purchaseService } from '../../services/purchaseService';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';
import { PAYMENT_MODES, PURCHASE_STATUS, PURCHASE_STATUS_CLASS } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';
import PurchaseFormModal from './PurchaseFormModal';

function ReceivePanel({ purchase, onDone, onCancel }) {
  const open = purchase.lignes.filter((l) => Number(l.quantite) > Number(l.quantiteRecue));
  const [rows, setRows] = useState(() => Object.fromEntries(open.map((l) => [l.id, {
    quantite: String(Number(l.quantite) - Number(l.quantiteRecue)), lot: '',
  }])));
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      const lignes = open.filter((l) => toMillimes(rows[l.id].quantite) > 0).map((l) => ({
        lineId: l.id, quantite: normalizeDecimal(rows[l.id].quantite), lotNumero: rows[l.id].lot || null,
      }));
      onDone(await purchaseService.receive(purchase.id, { lignes, notes: notes || null }));
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  return (
    <div className="panel" style={{ padding: 16, marginTop: 16 }}>
      <h2 style={{ marginBottom: 6 }}>Réception des marchandises</h2>
      <p className="muted" style={{ marginBottom: 12 }}>Indiquez les quantités réellement reçues. Le stock augmente à la validation de la réception.</p>
      <Alert type="error">{error}</Alert>
      <table className="table table-edit">
        <thead><tr><th>Article</th><th className="num">Reste à recevoir</th><th>Reçu</th><th>N° de lot</th></tr></thead>
        <tbody>
          {open.map((l) => (
            <tr key={l.id}>
              <td>{l.designation}</td>
              <td className="num">{formatQty(Number(l.quantite) - Number(l.quantiteRecue), l.unite)}</td>
              <td><input className="input" inputMode="decimal" value={rows[l.id].quantite}
                         onChange={(e) => setRows({ ...rows, [l.id]: { ...rows[l.id], quantite: e.target.value } })} /></td>
              <td>{l.suiviLot
                ? <input className="input" placeholder="Obligatoire" value={rows[l.id].lot}
                         onChange={(e) => setRows({ ...rows, [l.id]: { ...rows[l.id], lot: e.target.value } })} />
                : <span className="muted">—</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="field" style={{ marginTop: 12 }}>
        <label>Observations</label>
        <input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={255} />
      </div>
      <div className="actions">
        <Button variant="secondary" onClick={onCancel}>Fermer</Button>
        <Button onClick={submit} loading={busy}><PackageCheck size={16} />Valider la réception</Button>
      </div>
    </div>
  );
}

function PayPanel({ purchase, onDone, onCancel }) {
  const [form, setForm] = useState({ mode: 'VIREMENT', montant: String(purchase.reste), reference: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      onDone(await purchaseService.pay(purchase.id, { mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null }));
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };
  return (
    <div className="panel" style={{ padding: 16, marginTop: 16 }}>
      <h2 style={{ marginBottom: 12 }}>Paiement au fournisseur</h2>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        <div className="field"><label>Mode</label>
          <select className="select" value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
            {Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Montant (DT)</label>
          <input className="input" inputMode="decimal" value={form.montant} onChange={(e) => setForm({ ...form, montant: e.target.value })} />
          <span className="hint">Reste dû : {formatDT(purchase.reste)}</span></div>
        <div className="field wide"><label>Référence (n° chèque, virement…)</label>
          <input className="input" value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} maxLength={80} /></div>
      </div>
      <div className="actions">
        <Button variant="secondary" onClick={onCancel}>Fermer</Button>
        <Button onClick={submit} loading={busy} disabled={!toMillimes(form.montant)}><Wallet size={16} />Enregistrer le paiement</Button>
      </div>
    </div>
  );
}

export default function PurchaseDetailModal({ id, onClose, onChanged }) {
  const { can } = useAuth();
  const [p, setP] = useState(null);
  const [tab, setTab] = useState('lignes');
  const [panel, setPanel] = useState(null);
  const [error, setError] = useState('');
  const [motif, setMotif] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { purchaseService.get(id).then(setP).catch((e) => setError(e.message)); }, [id]);

  const apply = (updated) => {
    setP(updated);
    setPanel(null);
    onChanged?.();
  };
  const run = async (fn) => {
    setBusy(true);
    setError('');
    try { apply(await fn()); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };

  if (panel === 'edit') return <PurchaseFormModal purchase={p} onClose={() => setPanel(null)} onSaved={apply} />;

  const receivable = p && ['VALIDEE', 'PARTIELLEMENT_RECUE'].includes(p.statut);
  const payable = p && !['BROUILLON', 'ANNULEE'].includes(p.statut) && Number(p.reste) > 0;

  return (
    <Modal title={p ? `Commande ${p.numero}` : 'Commande'} onClose={onClose} wide
           footer={p ? (
             <>
               {p.statut === 'BROUILLON' && can('PURCHASE_CREATE') ? <Button variant="secondary" onClick={() => setPanel('edit')}><Pencil size={16} />Modifier</Button> : null}
               {['BROUILLON', 'VALIDEE'].includes(p.statut) && can('PURCHASE_VALIDATE') && Number(p.montantPaye) === 0
                 ? <Button variant="danger" onClick={() => setPanel('cancel')}><Ban size={16} />Annuler</Button> : null}
               {p.statut === 'BROUILLON' && can('PURCHASE_VALIDATE')
                 ? <Button loading={busy} onClick={() => run(() => purchaseService.validate(p.id))}><CheckCircle2 size={16} />Valider la commande</Button> : null}
               {receivable && can('PURCHASE_RECEIVE') ? <Button onClick={() => setPanel('receive')}><PackageCheck size={16} />Réceptionner</Button> : null}
               {payable && can('FINANCE_MANAGE') ? <Button onClick={() => setPanel('pay')}><Wallet size={16} />Payer</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!p ? <Spinner /> : (
        <>
          {p.statut === 'ANNULEE' ? <Alert type="error">Annulée par {p.annulePar} : {p.motifAnnulation}</Alert> : null}
          <dl className="kv" style={{ marginBottom: 16 }}>
            <dt>Fournisseur</dt><dd>{p.fournisseurNom}</dd>
            <dt>Date</dt><dd>{new Date(`${p.dateCommande}T12:00:00`).toLocaleDateString('fr-FR')}</dd>
            <dt>Statut</dt><dd><span className={`badge ${PURCHASE_STATUS_CLASS[p.statut]}`}>{PURCHASE_STATUS[p.statut]}</span></dd>
            <dt>Réf. facture</dt><dd>{p.refFacture || '—'}</dd>
            <dt>Total TTC</dt><dd><strong>{formatDT(p.totalTtc)}</strong> (HT {formatDT(p.totalHt)}, TVA {formatDT(p.totalTva)})</dd>
            <dt>Payé / reste</dt><dd>{formatDT(p.montantPaye)} / <strong>{formatDT(p.reste)}</strong></dd>
            <dt>Créée par</dt><dd>{p.creePar}, le {formatDateTime(p.createdAt)}</dd>
            {p.validePar ? <><dt>Validée par</dt><dd>{p.validePar}, le {formatDateTime(p.valideLe)}</dd></> : null}
          </dl>
          <Tabs value={tab} onChange={setTab} tabs={[
            { value: 'lignes', label: `Lignes (${p.lignes.length})` },
            { value: 'receptions', label: `Réceptions (${p.receptions.length})` },
            { value: 'paiements', label: `Paiements (${p.paiements.length})` },
          ]} />
          {tab === 'lignes' ? (
            <table className="table">
              <thead><tr><th>Article</th><th className="num">Commandé</th><th className="num">Reçu</th><th className="num">P.U. HT</th><th className="num">Remise</th><th className="num">TVA</th><th className="num">Total HT</th></tr></thead>
              <tbody>{p.lignes.map((l) => (
                <tr key={l.id}>
                  <td>{l.designation}</td><td className="num">{formatQty(l.quantite, l.unite)}</td>
                  <td className="num">{formatQty(l.quantiteRecue, l.unite)}</td><td className="num">{formatDT(l.prixUnitaire)}</td>
                  <td className="num">{Number(l.remisePct)} %</td><td className="num">{Number(l.tauxTva)} %</td><td className="num">{formatDT(l.totalHt)}</td>
                </tr>
              ))}</tbody>
            </table>
          ) : null}
          {tab === 'receptions' ? (p.receptions.length ? p.receptions.map((r) => (
            <div key={r.id} className="panel" style={{ padding: 12, marginBottom: 8 }}>
              <strong>{r.numero}</strong> <span className="muted">· {formatDateTime(r.date)} · {r.username}</span>
              <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
                {r.lignes.map((l, i) => <li key={i}>{l.designation} : {formatQty(l.quantite)}{l.lot ? ` (lot ${l.lot})` : ''}</li>)}
              </ul>
            </div>
          )) : <p className="muted">Aucune réception.</p>) : null}
          {tab === 'paiements' ? (p.paiements.length ? (
            <table className="table"><tbody>{p.paiements.map((x) => (
              <tr key={x.numero}><td><strong>{x.numero}</strong></td><td>{formatDateTime(x.date)}</td><td>{PAYMENT_MODES[x.mode]}</td>
                <td>{x.reference || '—'}</td><td className="num">{formatDT(x.montant)}</td></tr>
            ))}</tbody></table>
          ) : <p className="muted">Aucun paiement.</p>) : null}

          {panel === 'receive' ? <ReceivePanel purchase={p} onDone={apply} onCancel={() => setPanel(null)} /> : null}
          {panel === 'pay' ? <PayPanel purchase={p} onDone={apply} onCancel={() => setPanel(null)} /> : null}
          {panel === 'cancel' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              <div className="field"><label>Motif d'annulation *</label>
                <input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} autoFocus /></div>
              <div className="actions">
                <Button variant="secondary" onClick={() => setPanel(null)}>Ne pas annuler</Button>
                <Button variant="danger" disabled={!motif.trim()} loading={busy} onClick={() => run(() => purchaseService.cancel(p.id, motif.trim()))}>Confirmer l'annulation</Button>
              </div>
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}
