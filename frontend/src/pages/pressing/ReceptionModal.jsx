import { useCallback, useEffect, useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { clientService } from '../../services/clientService';
import { catalogService } from '../../services/catalogService';
import { settingsService } from '../../services/settingsService';
import { pressingService } from '../../services/operationsService';
import { formatDT, formatQty } from '../../utils/format';
import { OLIVE_STATES } from '../../utils/labels';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

/** Réception et pesée des olives d'un client. */
export default function ReceptionModal({ onClose, onDone }) {
  const [form, setForm] = useState({ clientId: '', articleId: '', variete: '', provenance: '', etatOlives: 'SAINES', poidsBrut: '', tare: '0', tarifKg: '', notes: '' });
  const [settings, setSettings] = useState(null);
  const [oilArticles, setOilArticles] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    settingsService.get().then((s) => {
      setSettings(s);
      setForm((f) => ({ ...f, tarifKg: s.tarifPressageKg != null ? String(s.tarifPressageKg) : '', articleId: s.pressageArticleId ?? '' }));
    }).catch(() => {});
    catalogService.articles({ actif: true, size: 100 }).then((p) => setOilArticles(p.content.filter((a) => a.suiviLot))).catch(() => {});
  }, []);

  const fetchClients = useCallback((q) => clientService.list({ q, actif: true, size: 30 }), []);
  const toClient = useCallback((c) => ({ value: c.id, label: `${c.nom} (${c.code})${c.telephone ? ` · ${c.telephone}` : ''}` }), []);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const netM = toMillimes(form.poidsBrut) - toMillimes(form.tare);
  const montantM = Math.round((netM * toMillimes(form.tarifKg)) / 1000);
  const valid = form.clientId && form.articleId && netM > 0 && form.tarifKg !== '';

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      onDone(await pressingService.receive({
        clientId: Number(form.clientId), articleId: Number(form.articleId), variete: form.variete || null,
        provenance: form.provenance || null, etatOlives: form.etatOlives, poidsBrut: normalizeDecimal(form.poidsBrut),
        tare: normalizeDecimal(form.tare) || '0', tarifKg: normalizeDecimal(form.tarifKg), notes: form.notes || null,
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title="Réception des olives d'un client" onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={submit} loading={saving} disabled={!valid}>Enregistrer la pesée</Button></>)}>
      <Alert type="error">{error}</Alert>
      {settings && (!settings.pressageArticleId || settings.tarifPressageKg == null)
        ? <Alert type="info">Astuce : définissez le tarif au kilo et l'article « huile des clients » dans Paramètres pour les préremplir.</Alert> : null}
      <RemoteSelect label="Client" required value={form.clientId} onChange={(id) => setForm({ ...form, clientId: id })}
                    fetcher={fetchClients} toOption={toClient} hint="Le client doit exister dans Clients (agriculteur ou particulier)." />
      <div className="form-grid">
        <div className="field"><label>Article huile des clients *</label>
          <select className="select" value={form.articleId} onChange={set('articleId')}>
            <option value="">Choisir…</option>
            {oilArticles.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode})</option>)}
          </select>
          <span className="hint">Article suivi par lot ; l'huile restera la propriété du client.</span></div>
        <div className="field"><label>État des olives *</label>
          <select className="select" value={form.etatOlives} onChange={set('etatOlives')}>
            {Object.entries(OLIVE_STATES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Variété</label><input className="input" value={form.variete} onChange={set('variete')} placeholder="Ex. Chemlali, Chetoui" /></div>
        <div className="field"><label>Provenance</label><input className="input" value={form.provenance} onChange={set('provenance')} /></div>
        <div className="field"><label>Poids brut (kg) *</label><input className="input pos-weight" inputMode="decimal" value={form.poidsBrut} onChange={set('poidsBrut')} autoFocus /></div>
        <div className="field"><label>Tare (kg)</label><input className="input" inputMode="decimal" value={form.tare} onChange={set('tare')} hint="Caisses, sacs" /></div>
        <div className="field"><label>Tarif du service (DT / kg) *</label><input className="input" inputMode="decimal" value={form.tarifKg} onChange={set('tarifKg')} /></div>
        <div className="field"><label>Observations</label><input className="input" value={form.notes} onChange={set('notes')} /></div>
      </div>
      <div className="weigh-summary">
        <div><span>Poids net</span><strong>{netM > 0 ? formatQty(fromMillimes(netM), 'kg') : '—'}</strong></div>
        <div><span>Montant du service</span><strong>{netM > 0 ? formatDT(fromMillimes(montantM)) : '—'}</strong></div>
      </div>
    </Modal>
  );
}
