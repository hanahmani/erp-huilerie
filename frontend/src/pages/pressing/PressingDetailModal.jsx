import { useEffect, useState } from 'react';
import { Ban, CheckCircle2, HandCoins, Play, Printer, Undo2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import DocPrintDialog from '../../components/print/DocPrintDialog';
import { useAuth } from '../../context/AuthContext';
import { pressingService } from '../../services/operationsService';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';
import { OLIVE_STATES, PAYMENT_MODES, PRESSING_STATUS, PRESSING_STATUS_CLASS } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';
import { ReceptionTicket, ReturnTicket } from './PressingTickets';

export default function PressingDetailModal({ id, initial, onClose, onChanged }) {
  const { can } = useAuth();
  const manage = can('PRESSING_MANAGE');
  const canPay = can(['PRESSING_MANAGE', 'SALES_CREATE']);
  const [p, setP] = useState(initial || null);
  const [panel, setPanel] = useState(null);
  const [form, setForm] = useState({});
  const [print, setPrint] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (!initial) pressingService.get(id).then(setP).catch((e) => setError(e.message)); }, [id, initial]);

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try { setP(await fn()); setPanel(null); setForm({}); onChanged?.(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const open = (name, defaults = {}) => { setPanel(name); setForm(defaults); setError(''); };
  const f = (k) => ({ value: form[k] ?? '', onChange: (e) => setForm({ ...form, [k]: e.target.value }) });

  if (print && p) {
    return <DocPrintDialog title={print === 'reception' ? 'Bon de pesée' : "Bon de remise d'huile"} onClose={() => setPrint(null)}
                           render={(format, settings) => (print === 'reception'
                             ? <ReceptionTicket p={p} settings={settings} format={format} />
                             : <ReturnTicket p={p} settings={settings} format={format} />)} />;
  }

  return (
    <Modal title={p ? `Pressage ${p.numero}` : 'Pressage'} onClose={onClose} wide
           footer={p ? (
             <>
               <Button variant="secondary" onClick={() => setPrint('reception')}><Printer size={16} />Bon de pesée</Button>
               {p.remises.length ? <Button variant="secondary" onClick={() => setPrint('return')}><Printer size={16} />Bon de remise</Button> : null}
               {manage && ['RECU', 'EN_COURS'].includes(p.statut) && !Number(p.montantPaye) ? <Button variant="danger" onClick={() => open('cancel')}><Ban size={16} />Annuler</Button> : null}
               {canPay && p.statut !== 'ANNULE' && Number(p.reste) > 0 ? <Button variant="secondary" onClick={() => open('pay', { mode: 'ESPECES', montant: String(p.reste) })}><HandCoins size={16} />Encaisser</Button> : null}
               {manage && p.statut === 'RECU' ? <Button loading={busy} onClick={() => run(() => pressingService.start(p.id))}><Play size={16} />Démarrer le pressage</Button> : null}
               {manage && p.statut === 'EN_COURS' ? <Button onClick={() => open('finish')}><CheckCircle2 size={16} />Terminer le pressage</Button> : null}
               {manage && p.statut === 'TERMINE' ? <Button onClick={() => open('return', { quantite: String(p.huileARemettre), remisA: p.clientNom })}><Undo2 size={16} />Remettre l'huile</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!p ? <Spinner /> : (
        <>
          {p.statut === 'ANNULE' ? <Alert type="error">Annulé : {p.motifAnnulation}</Alert> : null}
          {p.statut === 'TERMINE' && Number(p.reste) > 0 ? <Alert type="info">Encaissez le service ({formatDT(p.reste)}) avant de remettre l'huile.</Alert> : null}
          <div className="grid-2">
            <dl className="kv">
              <dt>Client</dt><dd>{p.clientNom} <span className="muted">{p.clientCode}{p.clientTelephone ? ` · ${p.clientTelephone}` : ''}</span></dd>
              <dt>Statut</dt><dd><span className={`badge ${PRESSING_STATUS_CLASS[p.statut]}`}>{PRESSING_STATUS[p.statut]}</span></dd>
              <dt>Reçu le</dt><dd>{formatDateTime(p.dateReception)} par {p.recuPar}</dd>
              <dt>Variété</dt><dd>{p.variete || '—'}{p.provenance ? ` · ${p.provenance}` : ''}</dd>
              <dt>État</dt><dd>{OLIVE_STATES[p.etatOlives]}</dd>
              <dt>Pesée</dt><dd>{formatQty(p.poidsBrut, 'kg')} − {formatQty(p.tare, 'kg')} = <strong>{formatQty(p.poidsNet, 'kg')}</strong></dd>
            </dl>
            <dl className="kv">
              <dt>Service</dt><dd>{formatDT(p.montant)} <span className="muted">({formatDT(p.tarifKg)}/kg)</span></dd>
              <dt>Payé / reste</dt><dd>{formatDT(p.montantPaye)} / <strong>{formatDT(p.reste)}</strong></dd>
              <dt>Huile obtenue</dt><dd>{p.huileObtenue != null ? <strong>{formatQty(p.huileObtenue, p.unite)}</strong> : '—'}</dd>
              <dt>Rendement</dt><dd>{p.rendement != null ? `${Number(p.rendement).toFixed(2)} ${p.unite} / 100 kg` : '—'}</dd>
              <dt>Remis / à remettre</dt><dd>{formatQty(p.huileRemise, p.unite)} / <strong>{formatQty(p.huileARemettre, p.unite)}</strong></dd>
              <dt>Lot client</dt><dd>{p.lotNumero || '—'}</dd>
            </dl>
          </div>

          {p.remises.length || p.paiements.length ? (
            <div className="grid-2" style={{ marginTop: 16 }}>
              <div><h2 style={{ marginBottom: 8 }}>Remises</h2>
                {p.remises.length ? <table className="table"><tbody>{p.remises.map((r) => (
                  <tr key={r.numero}><td><strong>{r.numero}</strong><div className="muted" style={{ fontSize: 12 }}>{formatDateTime(r.date)} · {r.remisA}</div></td><td className="num">{formatQty(r.quantite, p.unite)}</td></tr>
                ))}</tbody></table> : <p className="muted">Aucune remise.</p>}</div>
              <div><h2 style={{ marginBottom: 8 }}>Paiements</h2>
                {p.paiements.length ? <table className="table"><tbody>{p.paiements.map((x) => (
                  <tr key={x.numero}><td><strong>{x.numero}</strong><div className="muted" style={{ fontSize: 12 }}>{formatDateTime(x.date)} · {PAYMENT_MODES[x.mode]}</div></td><td className="num">{formatDT(x.montant)}</td></tr>
                ))}</tbody></table> : <p className="muted">Aucun paiement.</p>}</div>
            </div>
          ) : null}

          {panel ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              {panel === 'finish' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Fin du pressage</h2>
                  <div className="form-grid">
                    <div className="field"><label>Huile obtenue ({p.unite}) *</label><input className="input" inputMode="decimal" autoFocus {...f('huile')} /></div>
                    <div className="field"><label>Pertes (kg)</label><input className="input" inputMode="decimal" {...f('pertes')} /></div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!toMillimes(form.huile)} onClick={() => run(() => pressingService.finish(p.id, { huileObtenue: normalizeDecimal(form.huile), pertesKg: form.pertes ? normalizeDecimal(form.pertes) : null }))}>Enregistrer l'huile du client</Button></div>
                </>
              ) : null}
              {panel === 'pay' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Encaisser le service</h2>
                  <div className="form-grid">
                    <div className="field"><label>Mode</label><select className="select" {...f('mode')}>{Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
                    <div className="field"><label>Montant (DT)</label><input className="input" inputMode="decimal" {...f('montant')} /></div>
                    <div className="field wide"><label>Référence</label><input className="input" {...f('reference')} /></div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!toMillimes(form.montant)} onClick={() => run(() => pressingService.pay(p.id, { mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null }))}>Encaisser</Button></div>
                </>
              ) : null}
              {panel === 'return' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Remettre l'huile au client</h2>
                  <div className="form-grid">
                    <div className="field"><label>Quantité ({p.unite})</label><input className="input" inputMode="decimal" {...f('quantite')} />
                      <span className="hint">Reste à remettre : {formatQty(p.huileARemettre, p.unite)}</span></div>
                    <div className="field"><label>Remis à</label><input className="input" {...f('remisA')} /></div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!toMillimes(form.quantite)} onClick={() => run(() => pressingService.returnOil(p.id, { quantite: normalizeDecimal(form.quantite), remisA: form.remisA || null }))}>Valider la remise</Button></div>
                </>
              ) : null}
              {panel === 'cancel' ? (
                <>
                  <div className="field"><label>Motif d'annulation *</label><input className="input" autoFocus {...f('motif')} /></div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Ne pas annuler</Button>
                    <Button variant="danger" loading={busy} disabled={!(form.motif || '').trim()} onClick={() => run(() => pressingService.cancel(p.id, form.motif.trim()))}>Confirmer l'annulation</Button></div>
                </>
              ) : null}
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}
