import { useEffect, useState } from 'react';
import { ArrowRight, Ban, CheckCircle2, FileMinus, HandCoins, Pencil, Printer } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import DocPrintDialog from '../../components/print/DocPrintDialog';
import { useAuth } from '../../context/AuthContext';
import { salesDocService } from '../../services/salesDocService';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';
import { DOC_STATUS, DOC_STATUS_CLASS, DOC_TYPES, PAYMENT_MODES } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';
import SalesDocFormModal from './SalesDocFormModal';
import SalesDocPrint from './SalesDocPrint';

const NEXT = { DEVIS: ['COMMANDE', 'FACTURE'], COMMANDE: ['LIVRAISON', 'FACTURE'], LIVRAISON: ['FACTURE'] };

export default function SalesDocDetailModal({ id, onClose, onChanged, onOpen }) {
  const { can } = useAuth();
  const manage = can('SALESDOC_MANAGE');
  const [d, setD] = useState(null);
  const [panel, setPanel] = useState(null);
  const [form, setForm] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { setD(null); salesDocService.get(id).then(setD).catch((e) => setError(e.message)); }, [id]);

  const run = async (fn, openResult) => {
    setBusy(true);
    setError('');
    try {
      const r = await fn();
      onChanged?.();
      setPanel(null);
      if (openResult && r.id !== d.id) onOpen(r.id); else setD(r);
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const f = (k) => ({ value: form[k] ?? '', onChange: (e) => setForm({ ...form, [k]: e.target.value }) });

  if (panel === 'edit') return <SalesDocFormModal type={d.type} doc={d} onClose={() => setPanel(null)} onSaved={(x) => { setD(x); setPanel(null); onChanged?.(); }} />;
  if (panel === 'print') {
    return <DocPrintDialog title={`${DOC_TYPES[d.type]} ${d.numero || ''}`} onClose={() => setPanel(null)}
                           render={(format, settings) => <SalesDocPrint d={d} settings={settings} format={format} />} />;
  }

  const draft = d?.statut === 'BROUILLON';
  const valid = d?.statut === 'VALIDE';
  const isInvoice = d?.type === 'FACTURE';
  const canValidate = draft && manage && (!isInvoice || can('INVOICE_VALIDATE'));

  return (
    <Modal title={d ? `${DOC_TYPES[d.type]} ${d.numero || '(brouillon)'}` : 'Document'} onClose={onClose} wide
           footer={d ? (
             <>
               <Button variant="secondary" onClick={() => setPanel('print')}><Printer size={16} />Imprimer / PDF</Button>
               {manage && d.type !== 'AVOIR' && (draft || (valid && !isInvoice)) ? <Button variant="danger" onClick={() => { setPanel('cancel'); setForm({}); }}><Ban size={16} />Annuler</Button> : null}
               {draft && manage ? <Button variant="secondary" onClick={() => setPanel('edit')}><Pencil size={16} />Modifier</Button> : null}
               {canValidate ? <Button loading={busy} onClick={() => run(() => salesDocService.validate(d.id))}><CheckCircle2 size={16} />Valider</Button> : null}
               {valid && manage ? (NEXT[d.type] || []).map((t) => (
                 <Button key={t} variant="secondary" loading={busy} onClick={() => run(() => salesDocService.transform(d.id, t), true)}><ArrowRight size={16} />{DOC_TYPES[t]}</Button>
               )) : null}
               {valid && isInvoice && Number(d.reste) > 0 && can(['SALESDOC_MANAGE', 'FINANCE_MANAGE'])
                 ? <Button onClick={() => { setPanel('pay'); setForm({ mode: 'VIREMENT', montant: String(d.reste) }); }}><HandCoins size={16} />Encaisser</Button> : null}
               {valid && isInvoice && can('INVOICE_VALIDATE') ? <Button variant="danger" onClick={() => { setPanel('avoir'); setForm({ retour: true, mode: 'ESPECES' }); }}><FileMinus size={16} />Avoir</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!d ? <Spinner /> : (
        <>
          {d.statut === 'ANNULE' ? <Alert type="error">Annulé : {d.motifAnnulation}</Alert> : null}
          {draft && isInvoice && !can('INVOICE_VALIDATE') ? <Alert type="info">La validation d'une facture est réservée aux personnes autorisées.</Alert> : null}
          <div className="grid-2">
            <dl className="kv">
              <dt>Client</dt><dd>{d.clientNom} <span className="muted">{d.clientCode}</span></dd>
              <dt>Statut</dt><dd><span className={`badge ${DOC_STATUS_CLASS[d.statut]}`}>{DOC_STATUS[d.statut]}</span></dd>
              <dt>Date</dt><dd>{new Date(`${d.dateDoc}T12:00:00`).toLocaleDateString('fr-FR')}</dd>
              {d.echeance ? <><dt>Échéance</dt><dd>{new Date(`${d.echeance}T12:00:00`).toLocaleDateString('fr-FR')}</dd></> : null}
              {d.refClient ? <><dt>Réf. client</dt><dd>{d.refClient}</dd></> : null}
              <dt>Créé par</dt><dd>{d.creePar}, le {formatDateTime(d.createdAt)}</dd>
              {d.validePar ? <><dt>Validé par</dt><dd>{d.validePar}, le {formatDateTime(d.valideLe)}</dd></> : null}
            </dl>
            <dl className="kv">
              <dt>Total HT</dt><dd>{formatDT(d.totalHt)}</dd>
              <dt>TVA</dt><dd>{formatDT(d.totalTva)}</dd>
              {Number(d.timbre) ? <><dt>Timbre</dt><dd>{formatDT(d.timbre)}</dd></> : null}
              <dt>Total TTC</dt><dd><strong>{formatDT(d.totalTtc)}</strong></dd>
              {isInvoice ? <><dt>Payé / reste</dt><dd>{formatDT(d.montantPaye)} / <strong>{formatDT(d.reste)}</strong></dd></> : null}
              {d.source ? <><dt>Origine</dt><dd><button type="button" className="link-btn" onClick={() => onOpen(d.source.id)}>{DOC_TYPES[d.source.type]} {d.source.numero}</button></dd></> : null}
              {d.suites.length ? <><dt>Suite</dt><dd>{d.suites.map((s) => <button key={s.id} type="button" className="link-btn" onClick={() => onOpen(s.id)}>{DOC_TYPES[s.type]} {s.numero || '(brouillon)'}</button>)}</dd></> : null}
            </dl>
          </div>
          <table className="table" style={{ marginTop: 16 }}>
            <thead><tr><th>Article</th><th>Lot</th><th className="num">Qté</th><th className="num">P.U. HT</th><th className="num">Rem.</th><th className="num">TVA</th><th className="num">Total HT</th></tr></thead>
            <tbody>{d.lignes.map((l) => (
              <tr key={l.id}><td>{l.designation}</td><td>{l.lotNumero || '—'}</td><td className="num">{formatQty(l.quantite, l.unite)}</td>
                <td className="num">{formatDT(l.prixUnitaire)}</td><td className="num">{Number(l.remisePct)} %</td><td className="num">{Number(l.tauxTva)} %</td>
                <td className="num">{formatDT(l.totalHt)}</td></tr>
            ))}</tbody>
          </table>
          {d.paiements.length ? (
            <>
              <h2 style={{ margin: '16px 0 8px' }}>Paiements</h2>
              <table className="table"><tbody>{d.paiements.map((p) => (
                <tr key={p.numero}><td><strong>{p.numero}</strong></td><td>{formatDateTime(p.date)}</td><td>{PAYMENT_MODES[p.mode]}</td><td>{p.reference || ''}</td><td className="num">{formatDT(p.montant)}</td></tr>
              ))}</tbody></table>
            </>
          ) : null}

          {panel === 'pay' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              <div className="form-grid">
                <div className="field"><label>Mode</label><select className="select" {...f('mode')}>{Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
                <div className="field"><label>Montant (DT)</label><input className="input" inputMode="decimal" {...f('montant')} /></div>
                <div className="field wide"><label>Référence (chèque, virement…)</label><input className="input" {...f('reference')} /></div>
              </div>
              <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                <Button loading={busy} disabled={!toMillimes(form.montant)} onClick={() => run(() => salesDocService.pay(d.id, { mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null }))}>Encaisser</Button></div>
            </div>
          ) : null}
          {panel === 'avoir' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              <p style={{ marginBottom: 12 }}>L'avoir annule totalement la facture{Number(d.montantPaye) ? ` et rembourse ${formatDT(d.montantPaye)} déjà payés` : ''}.</p>
              <div className="field"><label>Motif *</label><input className="input" {...f('motif')} autoFocus /></div>
              <label className="check check-field"><input type="checkbox" checked={Boolean(form.retour)} onChange={(e) => setForm({ ...form, retour: e.target.checked })} />
                <span>Réintégrer la marchandise en stock<small>À cocher si les produits sont revenus (retour client).</small></span></label>
              {Number(d.montantPaye) ? <div className="field"><label>Mode de remboursement</label><select className="select" {...f('mode')}>{Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div> : null}
              <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                <Button variant="danger" loading={busy} disabled={!(form.motif || '').trim()}
                        onClick={() => run(() => salesDocService.creditNote(d.id, { motif: form.motif.trim(), retourStock: Boolean(form.retour), modeRemboursement: Number(d.montantPaye) ? form.mode : null }), true)}>Émettre l'avoir</Button></div>
            </div>
          ) : null}
          {panel === 'cancel' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              {d.type === 'LIVRAISON' && valid ? <p style={{ marginBottom: 10 }}>La marchandise livrée sera remise en stock.</p> : null}
              <div className="field"><label>Motif *</label><input className="input" {...f('motif')} autoFocus /></div>
              <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Ne pas annuler</Button>
                <Button variant="danger" loading={busy} disabled={!(form.motif || '').trim()} onClick={() => run(() => salesDocService.cancel(d.id, form.motif.trim()))}>Confirmer</Button></div>
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}
