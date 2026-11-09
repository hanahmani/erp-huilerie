import { useEffect, useState } from 'react';
import { Ban, BadgeDollarSign, BookUser, CalendarClock, CheckCircle2, HandCoins, Pencil, Play, Printer, Trash2, Undo2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import DocPrintDialog from '../../components/print/DocPrintDialog';
import { useAuth } from '../../context/AuthContext';
import { pressingService } from '../../services/operationsService';
import { tankService } from '../../services/stockService';
import { settingsService } from '../../services/settingsService';
import { formatDateTime, formatDT, formatQty, fromLocalInput, toLocalInput } from '../../utils/format';
import { OIL_DESTINATIONS, OLIVE_STATES, PAYMENT_MODES, PRESSING_MODES, TANK_TYPES, pressingStatusClass, pressingStatusLabel } from '../../utils/labels';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';
import { ReceptionTicket, ReturnTicket } from './PressingTickets';
import BulletinPressage, { BULLETIN_FORMATS } from './BulletinPressage';

const PRINT_TITLES = { bulletin: 'Bulletin de pressage', reception: 'Bon de pesée', return: "Bon de remise d'huile" };

/**
 * initialPrint : document à imprimer dès l'ouverture (ex. 'bulletin' juste après la réception).
 * onOpenAccount({ clientId, clientNom, tab }) : ouvre le compte du client (remises d'huile, avances).
 */
export default function PressingDetailModal({ id, initial, initialPrint, onClose, onChanged, onOpenAccount }) {
  const { can } = useAuth();
  const manage = can('PRESSING_MANAGE');
  const canPay = can(['PRESSING_MANAGE', 'SALES_CREATE']);
  const canPaySeller = can(['PRESSING_MANAGE', 'FINANCE_MANAGE']);
  const [tanks, setTanks] = useState([]);
  const [p, setP] = useState(initial || null);
  const [panel, setPanel] = useState(null);
  const [form, setForm] = useState({});
  const [print, setPrint] = useState(initialPrint || null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [density, setDensity] = useState(null);

  useEffect(() => { settingsService.get().then((s) => setDensity(Number(s.densiteHuile))).catch(() => {}); }, []);
  // Huile pesée en kg : équivalent en litres donné à titre indicatif (kg ÷ densité).
  const litres = (kg) => (density && kg != null && String(p?.unite).toLowerCase() === 'kg'
    ? <span className="muted"> · ≈ {formatQty((Number(kg) / density).toFixed(3), 'L')}</span> : null);

  useEffect(() => { if (!initial) pressingService.get(id).then(setP).catch((e) => setError(e.message)); }, [id, initial]);
  const achat = p?.mode === 'ACHAT';
  // Puits proposés à la fin du pressage, actifs, avec la place restante :
  // achat → puits de l'huilerie ; service → puits réservés à ce client et contenant la même huile.
  useEffect(() => {
    if (panel !== 'finish' || !p) return;
    tankService.list().then((l) => setTanks(l.filter((t) => t.actif && (achat
      ? !t.clientId : t.clientId === p.clientId && t.articleId === p.articleId)))).catch((e) => setError(e.message));
  }, [panel, achat, p]);
  const account = (tab) => onOpenAccount?.({ clientId: p.clientId, clientNom: p.clientNom, tab });
  // Annulation possible tant que rien n'a été encaissé ni payé en caisse (une avance déduite est rendue au vendeur).
  const cashPaid = p && (Number(p.montantPaye) > 0 || p.reglementsVendeur.some((x) => !x.avanceNumero));

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try { setP(await fn()); setPanel(null); setForm({}); onChanged?.(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const remove = async () => {
    setBusy(true);
    setError('');
    try { await pressingService.remove(p.id); onChanged?.(); onClose(); } catch (e) { setError(e.message); setBusy(false); }
  };
  const open = (name, defaults = {}) => { setPanel(name); setForm(defaults); setError(''); };
  const f = (k) => ({ value: form[k] ?? '', onChange: (e) => setForm({ ...form, [k]: e.target.value }) });

  if (print && p) {
    return <DocPrintDialog title={PRINT_TITLES[print]} onClose={() => setPrint(null)}
                           formats={print === 'bulletin' ? BULLETIN_FORMATS : undefined}
                           render={(format, settings) => {
                             if (print === 'bulletin') return <BulletinPressage p={p} settings={settings} format={format} />;
                             return print === 'reception'
                               ? <ReceptionTicket p={p} settings={settings} format={format} />
                               : <ReturnTicket p={p} settings={settings} format={format} />;
                           }} />;
  }

  return (
    <Modal title={p ? `Pressage ${p.numero} · bulletin n° ${p.numeroBulletin}` : 'Pressage'} onClose={onClose} wide
           footer={p ? (
             <>
               <Button variant="secondary" onClick={() => setPrint('bulletin')}><Printer size={16} />Imprimer le bulletin</Button>
               {!achat ? <Button variant="secondary" onClick={() => setPrint('reception')}><Printer size={16} />Bon de pesée</Button> : null}
               {manage && p.statut === 'RECU' ? <Button variant="secondary" onClick={() => open('transport', { dateReception: toLocalInput(p.dateReception), blNumero: p.blNumero || '', transporteur: p.transporteur || '', vehicule: p.vehicule || '', adresse: p.adresse || '' })}><Pencil size={16} />Corriger le bulletin</Button> : null}
               {manage && p.statut !== 'ANNULE' ? <Button variant="secondary" onClick={() => open('price', { prix: String(toMillimes(achat ? p.prixOlivesKg : p.tarifKg)) })}><BadgeDollarSign size={16} />{achat ? "Modifier le prix d'achat" : 'Modifier le tarif'}</Button> : null}
               {manage && p.statut !== 'RECU' ? <Button variant="secondary" onClick={() => open('date', { dateReception: toLocalInput(p.dateReception) })}><CalendarClock size={16} />Modifier la date</Button> : null}
               {p.remises.length ? <Button variant="secondary" onClick={() => setPrint('return')}><Printer size={16} />Bon de remise</Button> : null}
               {onOpenAccount ? <Button variant="secondary" onClick={() => account(achat ? 'seller' : 'oil')}><BookUser size={16} />Compte du client</Button> : null}
               {manage && ['RECU', 'EN_COURS'].includes(p.statut) && !cashPaid ? <Button variant="danger" onClick={() => open('cancel')}><Ban size={16} />Annuler</Button> : null}
               {manage && ['RECU', 'EN_COURS', 'ANNULE'].includes(p.statut) && !cashPaid ? <Button variant="danger" onClick={() => open('delete')}><Trash2 size={16} />Supprimer</Button> : null}
               {!achat && canPay && p.statut !== 'ANNULE' && Number(p.reste) > 0 ? <Button variant="secondary" onClick={() => open('pay', { mode: 'ESPECES', montant: String(p.reste) })}><HandCoins size={16} />Encaisser</Button> : null}
               {manage && p.statut === 'RECU' ? <Button loading={busy} onClick={() => run(() => pressingService.start(p.id))}><Play size={16} />Démarrer le pressage</Button> : null}
               {manage && p.statut === 'EN_COURS' ? <Button onClick={() => open('finish', { dest: '', mode: 'ESPECES' })}><CheckCircle2 size={16} />Terminer le pressage</Button> : null}
               {achat && canPaySeller && p.statut !== 'ANNULE' && Number(p.resteVendeur) > 0 ? <Button variant="secondary" onClick={() => open('paySeller', { mode: 'ESPECES', montant: String(p.resteVendeur) })}><HandCoins size={16} />Payer le vendeur</Button> : null}
               {!achat && manage && p.statut === 'TERMINE' && onOpenAccount ? <Button onClick={() => account('oil')}><Undo2 size={16} />Remettre l'huile</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!p ? <Spinner /> : (
        <>
          {p.statut === 'ANNULE' ? <Alert type="error">Annulé : {p.motifAnnulation}</Alert> : null}
          {!achat && p.statut === 'TERMINE' && Number(p.reste) > 0 ? <Alert type="info">Encaissez le service ({formatDT(p.reste)}) avant de remettre l'huile.</Alert> : null}
          <div className="grid-2">
            <dl className="kv">
              <dt>Opération</dt><dd><strong>{PRESSING_MODES[p.mode]}</strong></dd>
              <dt>{achat ? 'Vendeur' : 'Client'}</dt><dd>{p.clientNom} <span className="muted">{p.clientCode}{p.clientTelephone ? ` · ${p.clientTelephone}` : ''}</span></dd>
              <dt>Statut</dt><dd><span className={`badge ${pressingStatusClass(p)}`}>{pressingStatusLabel(p)}</span></dd>
              <dt>Reçu le</dt><dd>{formatDateTime(p.dateReception)} par {p.recuPar}</dd>
              <dt>Variété</dt><dd>{p.variete || '—'}{p.provenance ? ` · ${p.provenance}` : ''}</dd>
              <dt>État</dt><dd>{OLIVE_STATES[p.etatOlives]}</dd>
              <dt>Pesée</dt><dd>{formatQty(p.poidsBrut, 'kg')} − {formatQty(p.tare, 'kg')} = <strong>{formatQty(p.poidsNet, 'kg')}</strong></dd>
              <dt>Bulletin</dt><dd><strong>N° {p.numeroBulletin}</strong></dd>
              <dt>Adresse</dt><dd>{p.adresse || '—'}</dd>
              <dt>Bon de livraison</dt><dd>{p.blNumero || '—'}</dd>
              <dt>Transport</dt><dd>{p.transporteur || '—'}{p.vehicule ? ` · ${p.vehicule}` : ''}</dd>
            </dl>
            <dl className="kv">
              {achat ? (
                <>
                  <dt>Achat</dt><dd>{formatDT(p.montantAchat)} <span className="muted">({formatDT(p.prixOlivesKg)}/kg)</span></dd>
                  <dt>Payé / reste au vendeur</dt><dd>{formatDT(p.montantRegle)} / <strong>{formatDT(p.resteVendeur)}</strong></dd>
                  <dt>Puits</dt><dd>{p.tankCode ? <strong>{p.tankCode}</strong> : '—'}{p.tankNom ? ` · ${p.tankNom}` : ''}</dd>
                </>
              ) : (
                <>
                  <dt>Service</dt><dd>{formatDT(p.montant)} <span className="muted">({formatDT(p.tarifKg)}/kg)</span></dd>
                  <dt>Payé / reste</dt><dd>{formatDT(p.montantPaye)} / <strong>{formatDT(p.reste)}</strong></dd>
                </>
              )}
              <dt>Huile obtenue</dt><dd>{p.huileObtenue != null ? <><strong>{formatQty(p.huileObtenue, p.unite)}</strong>{litres(p.huileObtenue)}</> : '—'}</dd>
              <dt>Rendement</dt><dd>{p.rendement != null ? `${Number(p.rendement).toFixed(2)} ${p.unite} / 100 kg` : '—'}</dd>
              {!achat ? (
                <>
                  <dt>Remis / à remettre</dt><dd>{formatQty(p.huileRemise, p.unite)} / <strong>{formatQty(p.huileARemettre, p.unite)}</strong>{litres(p.huileARemettre)}</dd>
                  <dt>Lot client</dt><dd>{p.lotNumero || '—'}</dd>
                </>
              ) : null}
            </dl>
          </div>

          {achat && p.reglementsVendeur.length ? (
            <div style={{ marginTop: 16 }}><h2 style={{ marginBottom: 8 }}>Paiements au vendeur</h2>
              <table className="table"><tbody>{p.reglementsVendeur.map((x) => (
                <tr key={x.numero}><td><strong>{x.numero}</strong><div className="muted" style={{ fontSize: 12 }}>{formatDateTime(x.date)} · {x.avanceNumero ? `pris sur l'avance ${x.avanceNumero}` : PAYMENT_MODES[x.mode]}{x.reference && !x.avanceNumero ? ` · ${x.reference}` : ''}</div></td><td className="num">{formatDT(x.montant)}</td></tr>
              ))}</tbody></table></div>
          ) : null}

          {!achat && (p.remises.length || p.paiements.length) ? (
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
              {panel === 'transport' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Corriger le bulletin n° {p.numeroBulletin}</h2>
                  <div className="form-grid">
                    <div className="field"><label>Date de réception *</label><input className="input" type="datetime-local" max={toLocalInput()} {...f('dateReception')} /></div>
                    <div className="field"><label>N° bon de livraison</label><input className="input" maxLength={50} autoFocus {...f('blNumero')} /></div>
                    <div className="field"><label>Transporteur</label><input className="input" maxLength={120} {...f('transporteur')} /></div>
                    <div className="field"><label>N° véhicule</label><input className="input" maxLength={30} style={{ textTransform: 'uppercase' }} {...f('vehicule')} /></div>
                    <div className="field"><label>Adresse</label><input className="input" maxLength={255} {...f('adresse')} /></div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!form.dateReception} onClick={() => run(() => pressingService.updateTransport(p.id, {
                      blNumero: form.blNumero || null, transporteur: form.transporteur || null,
                      vehicule: form.vehicule || null, adresse: form.adresse || null,
                      dateReception: fromLocalInput(form.dateReception),
                    }))}>Enregistrer</Button></div>
                </>
              ) : null}
              {panel === 'price' ? (() => {
                const valid = /^\d+$/.test((form.prix || '').trim());
                const prixM = valid ? Number(form.prix.trim()) : 0;
                const montantM = Math.round((toMillimes(p.poidsNet) * prixM) / 1000);
                const payeM = toMillimes(achat ? p.montantRegle : p.montantPaye);
                return (
                  <>
                    <h2 style={{ marginBottom: 12 }}>{achat ? "Prix d'achat des olives" : 'Tarif du service'} · bulletin n° {p.numeroBulletin}</h2>
                    <div className="form-grid">
                      <div className="field"><label>{achat ? "Prix d'achat" : 'Tarif'} (millimes / kg d'olives) *</label>
                        <input className="input" inputMode="numeric" autoFocus {...f('prix')} />
                        {form.prix && !valid ? <span className="error">En millimes, sans virgule : 900 pour 0,900 DT.</span>
                          : <span className="hint">= {formatDT(fromMillimes(prixM))} le kg · actuel {formatDT(achat ? p.prixOlivesKg : p.tarifKg)}</span>}</div>
                      <div className="field"><label>Nouveau montant</label>
                        <div><strong>{formatDT(fromMillimes(montantM))}</strong> <span className="muted">({formatQty(p.poidsNet, 'kg')} × {formatDT(fromMillimes(prixM))})</span></div>
                        {payeM > 0 ? <span className={montantM < payeM ? 'error' : 'hint'}>Déjà payé : {formatDT(fromMillimes(payeM))}{montantM >= payeM ? ` · reste ${formatDT(fromMillimes(montantM - payeM))}` : ' : le montant ne peut pas être inférieur'}</span> : null}</div>
                    </div>
                    <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                      <Button loading={busy} disabled={!valid || montantM < payeM} onClick={() => run(() => pressingService.updatePrice(p.id, fromMillimes(prixM)))}>Enregistrer</Button></div>
                  </>
                );
              })() : null}
              {panel === 'date' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Date de réception du bulletin n° {p.numeroBulletin}</h2>
                  <div className="form-grid">
                    <div className="field"><label>Date de réception *</label>
                      <input className="input" type="datetime-local" autoFocus max={toLocalInput(p.dateDebut || undefined)} {...f('dateReception')} />
                      {p.dateDebut ? <span className="hint">Au plus tard le début du pressage ({formatDateTime(p.dateDebut)}).</span> : null}</div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!form.dateReception} onClick={() => run(() => pressingService.updateDate(p.id, fromLocalInput(form.dateReception)))}>Enregistrer</Button></div>
                </>
              ) : null}
              {panel === 'finish' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Fin du pressage</h2>
                  <div className="form-grid">
                    <div className="field"><label>Huile obtenue ({p.unite}) *</label><input className="input" inputMode="decimal" autoFocus {...f('huile')} /></div>
                    {achat ? (
                      <div className="field wide"><label>Puits / silo de stockage *</label>
                        <select className="select" {...f('tankId')}>
                          <option value="">Choisir…</option>
                          {tanks.map((t) => <option key={t.id} value={t.id}>{t.code} — {t.nom} · place restante {formatQty(t.placeRestante, t.unite)}</option>)}
                        </select>
                        {!tanks.length ? <span className="hint">Aucun puits actif : créez-en un dans Stock › Citernes / fûts / puits.</span> : null}</div>
                    ) : (
                      <>
                        <div className="field wide"><label>L'huile du client *</label>
                          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                            {Object.entries(OIL_DESTINATIONS).map(([k, v]) => (
                              <label key={k} className="check"><input type="radio" name="dest" value={k} checked={form.dest === k}
                                                                      onChange={(e) => setForm({ ...form, dest: e.target.value, tankId: '' })} />{v}</label>
                            ))}
                          </div></div>
                        {form.dest === 'TANK' ? (
                          <div className="field wide"><label>Citerne / fût du client *</label>
                            <select className="select" {...f('tankId')}>
                              <option value="">Choisir…</option>
                              {tanks.map((t) => <option key={t.id} value={t.id}>{TANK_TYPES[t.type]} {t.code} — {t.nom} · contient {formatQty(t.quantite, t.unite)} · place restante {formatQty(t.placeRestante, t.unite)}</option>)}
                            </select>
                            <span className="hint">{tanks.length ? "L'huile reste à l'huilerie, sur le compte du client ; elle sort plus tard par une remise."
                              : `Aucune citerne ni aucun fût réservé à ${p.clientNom} : créez-en dans Stock › Citernes / fûts / puits (propriétaire : ce client).`}</span></div>
                        ) : null}
                        {form.dest === 'LOT' ? <p className="hint field wide">L'huile reste à l'huilerie, sur le compte du client, dans un lot propre à ce pressage.</p> : null}
                        {form.dest === 'DIRECT' ? (
                          <>
                            <div className="field"><label>Remis à</label><input className="input" maxLength={150} placeholder={p.clientNom} {...f('remisA')} /></div>
                            {Number(p.reste) > 0 ? (
                              <>
                                <div className="field"><label>Paiement du service ({formatDT(p.reste)}) *</label>
                                  <select className="select" {...f('mode')}>{Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
                                <div className="field"><label>Référence</label><input className="input" maxLength={80} {...f('reference')} placeholder="N° de chèque, virement…" /></div>
                              </>
                            ) : null}
                            <p className="hint field wide">Le client emporte toute son huile maintenant{Number(p.reste) > 0 ? ` ; le service (${formatDT(p.reste)}) est encaissé en même temps` : ''}.</p>
                          </>
                        ) : null}
                      </>
                    )}
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!toMillimes(form.huile) || (achat ? !form.tankId : !form.dest || (form.dest === 'TANK' && !form.tankId))}
                            onClick={() => run(() => pressingService.finish(p.id, {
                              huileObtenue: normalizeDecimal(form.huile), pertesKg: null,
                              tankId: form.tankId ? Number(form.tankId) : null,
                              priseDirecte: !achat && form.dest === 'DIRECT',
                              modePaiement: !achat && form.dest === 'DIRECT' && Number(p.reste) > 0 ? form.mode : null,
                              reference: form.reference || null, remisA: form.remisA || null,
                            }))}>{achat ? "Verser l'huile dans le puits" : form.dest === 'DIRECT'
                              ? (Number(p.reste) > 0 ? "Encaisser et remettre l'huile" : "Remettre l'huile au client") : "Enregistrer l'huile du client"}</Button></div>
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
              {panel === 'paySeller' ? (
                <>
                  <h2 style={{ marginBottom: 12 }}>Payer le vendeur des olives</h2>
                  <div className="form-grid">
                    <div className="field"><label>Mode</label><select className="select" {...f('mode')}>{Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
                    <div className="field"><label>Montant (DT)</label><input className="input" inputMode="decimal" {...f('montant')} />
                      <span className="hint">Reste dû : {formatDT(p.resteVendeur)}</span></div>
                    <div className="field wide"><label>Référence</label><input className="input" {...f('reference')} placeholder="N° de chèque, virement…" /></div>
                  </div>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                    <Button loading={busy} disabled={!toMillimes(form.montant)} onClick={() => run(() => pressingService.paySeller(p.id, { mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null }))}>Payer</Button></div>
                </>
              ) : null}
              {panel === 'delete' ? (
                <>
                  <h2 style={{ marginBottom: 8 }}>Supprimer le bulletin n° {p.numeroBulletin} ?</h2>
                  <p className="muted">Le pressage {p.numero} de {p.clientNom} sera supprimé définitivement{achat && p.reglementsVendeur.length ? " ; l'avance déduite est rendue au vendeur" : ''}. Cette action est irréversible.</p>
                  <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Ne pas supprimer</Button>
                    <Button variant="danger" loading={busy} onClick={remove}><Trash2 size={16} />Supprimer définitivement</Button></div>
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
