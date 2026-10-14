import { useCallback, useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { catalogService } from '../../services/catalogService';
import { stockService } from '../../services/stockService';
import { MANUAL_MOVEMENTS, MOVEMENT_TYPES } from '../../utils/labels';
import { normalizeDecimal } from '../../utils/money';

const HELP = {
  STOCK_INITIAL: "Quantité présente au démarrage de l'ERP. Une seule fois par article ou par lot.",
  ENTREE: 'Arrivée de marchandise hors achat (don, transfert reçu…).',
  SORTIE: 'Sortie hors vente (casse, échantillon, usage interne…).',
  AJUSTEMENT_PLUS: "Correction après comptage : il y a plus que prévu.",
  AJUSTEMENT_MOINS: "Correction après comptage : il y a moins que prévu.",
};

export default function MovementModal({ onClose, onDone }) {
  const [article, setArticle] = useState(null);
  const [form, setForm] = useState({ articleId: '', lotId: '', type: 'ENTREE', quantite: '', motif: '' });
  const [lots, setLots] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchArticles = useCallback((q) => catalogService.articles({ q, actif: true, size: 50 }), []);
  const toOption = useCallback((a) => ({ value: a.id, label: `${a.nom} (${a.reference})` }), []);

  const pickArticle = async (id, item) => {
    setArticle(item);
    setForm((f) => ({ ...f, articleId: id, lotId: '' }));
    setLots([]);
    if (item?.suiviLot) {
      const page = await stockService.lots({ articleId: id, size: 100 });
      setLots(page.content);
    }
  };

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      await stockService.createMovement({
        articleId: Number(form.articleId),
        lotId: form.lotId ? Number(form.lotId) : null,
        type: form.type,
        quantite: normalizeDecimal(form.quantite),
        motif: form.motif.trim(),
      });
      onDone();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  const valid = form.articleId && form.quantite && form.motif.trim() && (!article?.suiviLot || form.lotId);

  return (
    <Modal title="Nouveau mouvement de stock" onClose={onClose}
           footer={(
             <>
               <Button variant="secondary" onClick={onClose}>Annuler</Button>
               <Button onClick={submit} loading={saving} disabled={!valid}>Enregistrer le mouvement</Button>
             </>
           )}>
      <Alert type="error">{error}</Alert>
      <RemoteSelect label="Article" required value={form.articleId} onChange={pickArticle}
                    fetcher={fetchArticles} toOption={toOption} />
      {article?.suiviLot ? (
        <div className="field">
          <label>Lot *</label>
          <select className="select" value={form.lotId} onChange={(e) => setForm({ ...form, lotId: e.target.value })}>
            <option value="">Choisir un lot…</option>
            {lots.map((l) => <option key={l.id} value={l.id}>{l.numero} · {l.proprietaire === 'CLIENT' ? `client ${l.clientNom}` : 'huilerie'}</option>)}
          </select>
          {!lots.length ? <span className="hint">Aucun lot : créez-le dans l'onglet Lots.</span> : null}
        </div>
      ) : null}
      <div className="field">
        <label>Type de mouvement *</label>
        <select className="select" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
          {MANUAL_MOVEMENTS.map((t) => <option key={t} value={t}>{MOVEMENT_TYPES[t]}</option>)}
        </select>
        <span className="hint">{HELP[form.type]}</span>
      </div>
      <div className="field">
        <label>Quantité {article ? `(${article.uniteCode})` : ''} *</label>
        <input className="input" inputMode="decimal" value={form.quantite}
               onChange={(e) => setForm({ ...form, quantite: e.target.value })} />
      </div>
      <div className="field">
        <label>Motif *</label>
        <input className="input" value={form.motif} maxLength={255}
               onChange={(e) => setForm({ ...form, motif: e.target.value })}
               placeholder="Obligatoire : il apparaît dans l'historique" />
      </div>
    </Modal>
  );
}
