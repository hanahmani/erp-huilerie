import { useCallback, useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { catalogService } from '../../services/catalogService';
import { clientService } from '../../services/clientService';
import { stockService } from '../../services/stockService';

export default function LotModal({ onClose, onDone }) {
  const [form, setForm] = useState({ articleId: '', numero: '', proprietaire: 'ENTREPRISE', clientId: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchArticles = useCallback(async (q) => {
    const page = await catalogService.articles({ q, actif: true, size: 50 });
    return page.content.filter((a) => a.suiviLot);
  }, []);
  const toArticle = useCallback((a) => ({ value: a.id, label: `${a.nom} (${a.reference})` }), []);
  const fetchClients = useCallback((q) => clientService.list({ q, actif: true, size: 50 }), []);
  const toClient = useCallback((c) => ({ value: c.id, label: `${c.nom} (${c.code})` }), []);

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      await stockService.createLot({
        articleId: Number(form.articleId),
        numero: form.numero.trim(),
        proprietaire: form.proprietaire,
        clientId: form.proprietaire === 'CLIENT' ? Number(form.clientId) : null,
      });
      onDone();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  const valid = form.articleId && form.numero.trim() && (form.proprietaire === 'ENTREPRISE' || form.clientId);

  return (
    <Modal title="Nouveau lot" onClose={onClose}
           footer={(
             <>
               <Button variant="secondary" onClick={onClose}>Annuler</Button>
               <Button onClick={submit} loading={saving} disabled={!valid}>Créer le lot</Button>
             </>
           )}>
      <Alert type="error">{error}</Alert>
      <RemoteSelect label="Article suivi par lot" required value={form.articleId}
                    onChange={(id) => setForm({ ...form, articleId: id })} fetcher={fetchArticles} toOption={toArticle} />
      <div className="field">
        <label>Numéro de lot *</label>
        <input className="input" value={form.numero} maxLength={50} onChange={(e) => setForm({ ...form, numero: e.target.value })} />
      </div>
      <div className="field">
        <label>Propriétaire de l'huile *</label>
        <div className="checks">
          <label className="check">
            <input type="radio" name="owner" checked={form.proprietaire === 'ENTREPRISE'}
                   onChange={() => setForm({ ...form, proprietaire: 'ENTREPRISE', clientId: '' })} />
            Huilerie (vendable)
          </label>
          <label className="check">
            <input type="radio" name="owner" checked={form.proprietaire === 'CLIENT'}
                   onChange={() => setForm({ ...form, proprietaire: 'CLIENT' })} />
            Client (pressage, non vendable)
          </label>
        </div>
      </div>
      {form.proprietaire === 'CLIENT' ? (
        <RemoteSelect label="Client propriétaire" required value={form.clientId}
                      onChange={(id) => setForm({ ...form, clientId: id })} fetcher={fetchClients} toOption={toClient} />
      ) : null}
    </Modal>
  );
}
