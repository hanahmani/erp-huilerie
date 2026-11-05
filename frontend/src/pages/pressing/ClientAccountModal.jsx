import { useCallback, useEffect, useState } from 'react';
import { HandCoins, Undo2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Tabs from '../../components/common/Tabs';
import StatCard from '../../components/common/StatCard';
import { useAuth } from '../../context/AuthContext';
import { pressingService } from '../../services/operationsService';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';
import { PAYMENT_MODES, PRESSING_STATUS } from '../../utils/labels';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

/** Compte d'huile (pressage à façon) : solde, puits du client, remise en kg ou litres, relevé des entrées et sorties. */
function OilTab({ clientId, clientNom, onChanged }) {
  const { can } = useAuth();
  const [acc, setAcc] = useState(null);
  const [form, setForm] = useState({ quantite: '', enLitres: true, remisA: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => pressingService.oilAccount(clientId).then(setAcc).catch((e) => setError(e.message)), [clientId]);
  useEffect(() => { load(); }, [load]);

  if (!acc) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  const kg = acc.unite.toLowerCase() === 'kg';
  const qM = toMillimes(form.quantite);
  const kgM = form.enLitres ? Math.round(qM * Number(acc.densite)) : qM;
  const due = Number(acc.montantDu) > 0;

  const submit = async () => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const next = await pressingService.returnToClient(clientId, {
        quantite: normalizeDecimal(form.quantite), enLitres: kg && form.enLitres, remisA: form.remisA || null,
      });
      const last = next.mouvements[next.mouvements.length - 1];
      setAcc(next);
      setForm({ quantite: '', enLitres: form.enLitres, remisA: '' });
      setMessage(`Remise ${last?.numero ?? ''} enregistrée.`);
      onChanged?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="stats">
        <StatCard label="Huile à remettre" value={formatQty(acc.solde, acc.unite)}
                  hint={kg && acc.soldeLitres != null ? `≈ ${formatQty(acc.soldeLitres, 'L')} (1 L = ${Number(acc.densite).toLocaleString('fr-FR')} kg)` : null} />
        <StatCard label="Service encore dû" value={formatDT(acc.montantDu)} tone={due ? 'warn' : undefined}
                  hint={due ? "À encaisser avant toute remise d'huile" : 'Rien à payer'} />
        {acc.puits.map((t) => (
          <StatCard key={t.id} label={`Puits ${t.code}`} value={formatQty(t.quantite, acc.unite)}
                    hint={`${t.nom} · capacité ${formatQty(t.capacite, acc.unite)}`} />
        ))}
      </div>

      <Alert type="error">{error}</Alert>
      {message ? <Alert type="success">{message}</Alert> : null}

      {can('PRESSING_MANAGE') && Number(acc.solde) > 0 ? (
        <div className="panel" style={{ padding: 16, marginBottom: 16 }}>
          <h2 style={{ marginBottom: 12 }}>Remettre de l'huile à {clientNom}</h2>
          {due ? <Alert type="info">Encaissez d'abord le service ({formatDT(acc.montantDu)}) depuis le bulletin du pressage.</Alert> : null}
          <div className="form-grid">
            <div className="field"><label>Quantité *</label>
              <input className="input" inputMode="decimal" value={form.quantite} autoFocus
                     onChange={(e) => setForm({ ...form, quantite: e.target.value })} />
              {kg ? (
                <div className="pos-unit" role="group" aria-label="Unité de la remise">
                  <button type="button" className={form.enLitres ? 'active' : ''} onClick={() => setForm({ ...form, enLitres: true })}>Litre</button>
                  <button type="button" className={form.enLitres ? '' : 'active'} onClick={() => setForm({ ...form, enLitres: false })}>kg</button>
                  {form.enLitres && qM > 0 ? <span className="muted">= {formatQty(fromMillimes(kgM), 'kg')}</span> : null}
                </div>
              ) : null}
              <span className="hint">Disponible : {formatQty(acc.solde, acc.unite)}</span></div>
            <div className="field"><label>Remis à</label>
              <input className="input" value={form.remisA} placeholder={clientNom} onChange={(e) => setForm({ ...form, remisA: e.target.value })} /></div>
          </div>
          <div className="actions">
            <Button loading={busy} disabled={!qM || due} onClick={submit}><Undo2 size={16} />Valider la remise</Button>
          </div>
        </div>
      ) : null}

      <h2 style={{ marginBottom: 8 }}>Relevé d'huile</h2>
      {acc.mouvements.length ? (
        <div className="table-wrap" style={{ border: '1px solid var(--beige-300)', borderRadius: 8 }}>
          <table className="table">
            <thead><tr><th>Date</th><th>Opération</th><th className="num">Entrée</th><th className="num">Sortie</th><th className="num">Solde</th></tr></thead>
            <tbody>{acc.mouvements.map((m, i) => (
              <tr key={`${m.numero}-${i}`}>
                <td>{formatDateTime(m.date)}<div className="muted" style={{ fontSize: 12 }}>{m.username}</div></td>
                <td><strong>{m.type === 'ENTREE' ? 'Pressage' : 'Remise'} {m.numero}</strong><div className="muted" style={{ fontSize: 12 }}>{m.detail}</div></td>
                <td className="num">{m.entree != null ? formatQty(m.entree, acc.unite) : ''}</td>
                <td className="num">{m.sortie != null ? <>{formatQty(m.sortie, acc.unite)}{m.litres != null ? <div className="muted" style={{ fontSize: 12 }}>{formatQty(m.litres, 'L')}</div> : null}</> : ''}</td>
                <td className="num"><strong>{formatQty(m.solde, acc.unite)}</strong></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      ) : <p className="muted">Aucun pressage à façon terminé pour ce client.</p>}
    </>
  );
}

/** Compte vendeur (achat d'olives) : avances versées et disponibles, achats et reste dû. */
function SellerTab({ clientId, onChanged }) {
  const { can } = useAuth();
  const canPay = can(['PRESSING_MANAGE', 'FINANCE_MANAGE']);
  const [acc, setAcc] = useState(null);
  const [form, setForm] = useState({ mode: 'ESPECES', montant: '', reference: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => pressingService.sellerAccount(clientId).then(setAcc).catch((e) => setError(e.message)), [clientId]);
  useEffect(() => { load(); }, [load]);

  if (!acc) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  const submit = async () => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      setAcc(await pressingService.giveAdvance(clientId, {
        mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null,
      }));
      setForm({ mode: form.mode, montant: '', reference: '' });
      setMessage('Avance enregistrée : elle sera déduite des achats d\'olives de ce vendeur.');
      onChanged?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="stats">
        <StatCard label="Reste dû au vendeur" value={formatDT(acc.resteDu)} tone={Number(acc.resteDu) > 0 ? 'warn' : undefined} />
        <StatCard label="Avance disponible" value={formatDT(acc.avanceDisponible)} hint="Déduite automatiquement du prochain achat" />
      </div>

      <Alert type="error">{error}</Alert>
      {message ? <Alert type="success">{message}</Alert> : null}

      {canPay ? (
        <div className="panel" style={{ padding: 16, marginBottom: 16 }}>
          <h2 style={{ marginBottom: 12 }}>Donner une avance</h2>
          <div className="form-grid">
            <div className="field"><label>Montant (DT) *</label>
              <input className="input" inputMode="decimal" value={form.montant} onChange={(e) => setForm({ ...form, montant: e.target.value })} /></div>
            <div className="field"><label>Mode</label>
              <select className="select" value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}>
                {Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select></div>
            <div className="field wide"><label>Référence</label>
              <input className="input" value={form.reference} placeholder="N° de chèque, virement…" onChange={(e) => setForm({ ...form, reference: e.target.value })} /></div>
          </div>
          <span className="hint">Sortie de caisse maintenant ; déduite d'abord des achats pas encore payés, puis des prochains.</span>
          <div className="actions">
            <Button loading={busy} disabled={!toMillimes(form.montant)} onClick={submit}><HandCoins size={16} />Verser l'avance</Button>
          </div>
        </div>
      ) : null}

      <div className="grid-2">
        <div><h2 style={{ marginBottom: 8 }}>Avances</h2>
          {acc.avances.length ? <table className="table"><tbody>{acc.avances.map((a) => (
            <tr key={a.numero}>
              <td><strong>{a.numero}</strong><div className="muted" style={{ fontSize: 12 }}>{formatDateTime(a.date)} · {PAYMENT_MODES[a.mode]}{a.reference ? ` · ${a.reference}` : ''}</div></td>
              <td className="num">{formatDT(a.montant)}<div className="muted" style={{ fontSize: 12 }}>reste {formatDT(a.reste)}</div></td>
            </tr>
          ))}</tbody></table> : <p className="muted">Aucune avance.</p>}</div>
        <div><h2 style={{ marginBottom: 8 }}>Achats d'olives</h2>
          {acc.achats.length ? <table className="table"><tbody>{acc.achats.map((x) => (
            <tr key={x.id}>
              <td><strong>{x.numero}</strong> <span className="muted">bulletin {x.numeroBulletin}</span>
                <div className="muted" style={{ fontSize: 12 }}>{formatDateTime(x.date)} · {formatQty(x.poidsNet, 'kg')} × {formatDT(x.prixOlivesKg)}{x.statut === 'ANNULE' ? ` · ${PRESSING_STATUS.ANNULE}` : ''}</div></td>
              <td className="num">{formatDT(x.montantAchat)}<div className="muted" style={{ fontSize: 12 }}>reste {formatDT(x.reste)}</div></td>
            </tr>
          ))}</tbody></table> : <p className="muted">Aucun achat d'olives.</p>}</div>
      </div>
    </>
  );
}

/** Compte d'un client du pressage : huile (pressage à façon) et vendeur (achat d'olives, avances). */
export default function ClientAccountModal({ clientId, clientNom, initialTab = 'oil', onClose, onChanged }) {
  const [tab, setTab] = useState(initialTab);
  return (
    <Modal title={`Compte de ${clientNom}`} onClose={onClose} wide>
      <Tabs tabs={[{ value: 'oil', label: "Huile (pressage à façon)" }, { value: 'seller', label: "Vendeur d'olives (achats, avances)" }]}
            value={tab} onChange={setTab} />
      <div style={{ marginTop: 16 }}>
        {tab === 'oil'
          ? <OilTab clientId={clientId} clientNom={clientNom} onChanged={onChanged} />
          : <SellerTab clientId={clientId} onChanged={onChanged} />}
      </div>
    </Modal>
  );
}
