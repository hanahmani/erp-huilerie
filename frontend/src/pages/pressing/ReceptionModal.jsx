import { useCallback, useEffect, useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { clientService } from '../../services/clientService';
import { catalogService } from '../../services/catalogService';
import { settingsService } from '../../services/settingsService';
import { pressingService } from '../../services/operationsService';
import { formatDT, formatQty, fromLocalInput, toLocalInput } from '../../utils/format';
import { OLIVE_STATES, PAYMENT_MODES, PRESSING_MODES } from '../../utils/labels';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

/** Tarif du pressage à façon proposé quand Paramètres n'en définit pas : 200 millimes (0,200 DT) le kg. */
const DEFAULT_TARIF_MILLIMES = 200;

/** Bulletin de pressage : réception et pesée des olives, pressées pour le client ou achetées par l'huilerie. */
export default function ReceptionModal({ onClose, onDone }) {
  const [form, setForm] = useState({ clientId: '', articleId: '', variete: '', provenance: '', etatOlives: 'SAINES', poidsBrut: '', tare: '0', tarifMillimes: String(DEFAULT_TARIF_MILLIMES), notes: '',
    numeroCarnet: '', blNumero: '', transporteur: '', vehicule: '', adresse: '', mode: 'SERVICE', prixMillimes: '', avance: '', modeAvance: 'ESPECES',
    dateReception: toLocalInput() });
  const [avanceDispo, setAvanceDispo] = useState(0);   // avances déjà versées au vendeur choisi, en millimes
  const [clientAdresse, setClientAdresse] = useState('');   // adresse proposée par la fiche client
  const [settings, setSettings] = useState(null);
  const [oilArticles, setOilArticles] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    settingsService.get().then((s) => {
      setSettings(s);
      setForm((f) => ({ ...f, tarifMillimes: s.tarifPressageKg != null ? String(toMillimes(s.tarifPressageKg)) : f.tarifMillimes,
        articleId: s.pressageArticleId ?? '' }));
    }).catch(() => {});
    catalogService.articles({ actif: true, size: 100 }).then((p) => setOilArticles(p.content.filter((a) => a.suiviLot))).catch(() => {});
  }, []);

  const fetchClients = useCallback((q) => clientService.list({ q, actif: true, size: 30 }), []);
  const toClient = useCallback((c) => ({ value: c.id, label: `${c.nom} (${c.code})${c.telephone ? ` · ${c.telephone}` : ''}` }), []);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  // L'adresse suit le client choisi tant qu'elle n'a pas été modifiée à la main.
  const chooseClient = (id, client) => {
    const proposed = client?.adresse || '';
    setForm((f) => ({ ...f, clientId: id, adresse: !f.adresse || f.adresse === clientAdresse ? proposed : f.adresse }));
    setClientAdresse(proposed);
  };

  const achat = form.mode === 'ACHAT';
  useEffect(() => {
    if (!achat || !form.clientId) { setAvanceDispo(0); return; }
    pressingService.sellerAccount(form.clientId).then((a) => setAvanceDispo(toMillimes(a.avanceDisponible))).catch(() => setAvanceDispo(0));
  }, [achat, form.clientId]);

  // Prix d'achat saisi en millimes entiers (900 = 0,900 DT le kg), enregistré en dinars.
  const prixValid = /^\d+$/.test(form.prixMillimes.trim());
  const prixM = prixValid ? Number(form.prixMillimes.trim()) : 0;
  // Tarif du service, en millimes entiers aussi (200 = 0,200 DT le kg).
  const tarifValid = /^\d+$/.test(form.tarifMillimes.trim());
  const tarifM = tarifValid ? Number(form.tarifMillimes.trim()) : 0;
  const netM = toMillimes(form.poidsBrut) - toMillimes(form.tare);
  const montantM = Math.round((netM * (achat ? prixM : tarifM)) / 1000);
  const deduitM = achat ? Math.min(avanceDispo, montantM) : 0;
  const avanceM = achat ? toMillimes(form.avance) : 0;
  const resteM = montantM - deduitM - avanceM;
  const dateValid = !!form.dateReception && new Date(form.dateReception) <= new Date(Date.now() + 5 * 60000);
  const valid = form.clientId && netM > 0 && dateValid && (achat ? prixValid && resteM >= 0 : form.articleId && tarifValid);

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      onDone(await pressingService.receive({
        clientId: Number(form.clientId), articleId: achat ? null : Number(form.articleId), variete: form.variete || null,
        provenance: form.provenance || null, etatOlives: form.etatOlives, poidsBrut: normalizeDecimal(form.poidsBrut),
        tare: normalizeDecimal(form.tare) || '0', tarifKg: achat ? null : fromMillimes(tarifM), notes: form.notes || null,
        mode: form.mode, prixOlivesKg: achat ? fromMillimes(prixM) : null,
        avance: achat && avanceM > 0 ? fromMillimes(avanceM) : null, modeAvance: achat && avanceM > 0 ? form.modeAvance : null,
        numeroCarnet: form.numeroCarnet.trim() || null,
        blNumero: form.blNumero || null, transporteur: form.transporteur || null, vehicule: form.vehicule || null,
        adresse: form.adresse || null, dateReception: fromLocalInput(form.dateReception),
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title="Bulletin de pressage — réception des olives" onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={submit} loading={saving} disabled={!valid}>Enregistrer la pesée</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="field">
        <label>Type d'opération *</label>
        <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
          {Object.entries(PRESSING_MODES).map(([k, v]) => (
            <label key={k} className="check"><input type="radio" name="mode" value={k} checked={form.mode === k} onChange={set('mode')} />{v}</label>
          ))}
        </div>
        <span className="hint">{achat
          ? "L'huilerie achète les olives au kilo : après le pressage, l'huile sera versée dans une pile, une citerne ou un fût et vendue en caisse."
          : "Le client reprend son huile : elle reste sa propriété et n'est jamais vendue."}</span>
      </div>
      {!achat && settings && !settings.pressageArticleId
        ? <Alert type="info">Astuce : définissez l'article « huile des clients » dans Paramètres pour le préremplir.</Alert> : null}
      <RemoteSelect label="Client" required value={form.clientId} onChange={chooseClient}
                    fetcher={fetchClients} toOption={toClient} hint="Le client doit exister dans Clients (agriculteur ou particulier)." />
      <div className="form-grid">
        {!achat ? (
          <div className="field"><label>Article huile des clients *</label>
            <select className="select" value={form.articleId} onChange={set('articleId')}>
              <option value="">Choisir…</option>
              {oilArticles.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode})</option>)}
            </select>
            <span className="hint">Article suivi par lot ; l'huile restera la propriété du client.</span></div>
        ) : null}
        <div className="field"><label>Date de réception *</label>
          <input className="input" type="datetime-local" value={form.dateReception} onChange={set('dateReception')} max={toLocalInput()} />
          {form.dateReception && !dateValid ? <span className="error">La date ne peut pas être dans le futur.</span>
            : <span className="hint">Maintenant par défaut ; modifiable.</span>}</div>
        <div className="field"><label>État des olives *</label>
          <select className="select" value={form.etatOlives} onChange={set('etatOlives')}>
            {Object.entries(OLIVE_STATES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Variété</label><input className="input" value={form.variete} onChange={set('variete')} placeholder="Ex. Chemlali, Chetoui" /></div>
        <div className="field"><label>Provenance</label><input className="input" value={form.provenance} onChange={set('provenance')} /></div>
        <div className="field"><label>Poids brut (kg) *</label><input className="input pos-weight" inputMode="decimal" value={form.poidsBrut} onChange={set('poidsBrut')} autoFocus /></div>
        <div className="field"><label>Tare (kg)</label><input className="input" inputMode="decimal" value={form.tare} onChange={set('tare')} hint="Caisses, sacs" /></div>
        {achat
          ? <div className="field"><label>Prix d'achat (millimes / kg d'olives) *</label>
              <input className="input" inputMode="numeric" value={form.prixMillimes} onChange={set('prixMillimes')} placeholder="Ex. 900" />
              {form.prixMillimes && !prixValid ? <span className="error">En millimes, sans virgule : 900 pour 0,900 DT.</span>
                : <span className="hint">{prixValid ? `= ${formatDT(fromMillimes(prixM))} le kg` : '900 millimes = 0,900 DT le kg'}</span>}</div>
          : <div className="field"><label>Tarif du service (millimes / kg d'olives) *</label>
              <input className="input" inputMode="numeric" value={form.tarifMillimes} onChange={set('tarifMillimes')} placeholder={String(DEFAULT_TARIF_MILLIMES)} />
              {form.tarifMillimes && !tarifValid ? <span className="error">En millimes, sans virgule : 200 pour 0,200 DT.</span>
                : <span className="hint">{tarifValid ? `= ${formatDT(fromMillimes(tarifM))} le kg ; modifiable` : '200 millimes = 0,200 DT le kg'}</span>}</div>}
        {achat ? (
          <>
            <div className="field"><label>Avance payée aujourd'hui (DT)</label>
              <input className="input" inputMode="decimal" value={form.avance} onChange={set('avance')} placeholder="0,000" />
              <span className="hint">Sortie de caisse maintenant ; le reste se paie plus tard depuis le bulletin.</span></div>
            <div className="field"><label>Mode de l'avance</label>
              <select className="select" value={form.modeAvance} onChange={set('modeAvance')}>
                {Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select></div>
          </>
        ) : null}
        <div className="field"><label>Observations</label><input className="input" value={form.notes} onChange={set('notes')} /></div>
        <div className="field"><label>N° carnet</label><input className="input" maxLength={20} value={form.numeroCarnet} onChange={set('numeroCarnet')} />
          <span className="hint">N° du bulletin papier dans le carnet.</span></div>
        <div className="field"><label>N° bon de livraison</label><input className="input" maxLength={50} value={form.blNumero} onChange={set('blNumero')} /></div>
        <div className="field"><label>Transporteur</label><input className="input" maxLength={120} value={form.transporteur} onChange={set('transporteur')} /></div>
        <div className="field"><label>N° véhicule</label><input className="input" maxLength={30} value={form.vehicule} onChange={set('vehicule')}
                                                              style={{ textTransform: 'uppercase' }} placeholder="Ex. 123 TU 4567" /></div>
        <div className="field"><label>Adresse</label><input className="input" maxLength={255} value={form.adresse} onChange={set('adresse')} />
          <span className="hint">Reprise de la fiche client ; modifiable.</span></div>
      </div>
      <div className="weigh-summary">
        <div><span>Poids net</span><strong>{netM > 0 ? formatQty(fromMillimes(netM), 'kg') : '—'}</strong></div>
        <div><span>{achat ? "Montant de l'achat" : 'Montant du service'}</span><strong>{netM > 0 ? formatDT(fromMillimes(montantM)) : '—'}</strong></div>
        {achat && deduitM > 0 ? <div><span>Avance déjà versée, déduite</span><strong>− {formatDT(fromMillimes(deduitM))}</strong></div> : null}
        {achat && avanceM > 0 ? <div><span>Avance payée aujourd'hui</span><strong>− {formatDT(fromMillimes(avanceM))}</strong></div> : null}
        {achat && netM > 0 ? <div><span>Reste dû au vendeur</span><strong>{resteM >= 0 ? formatDT(fromMillimes(resteM)) : "L'avance dépasse le montant"}</strong></div> : null}
      </div>
    </Modal>
  );
}
