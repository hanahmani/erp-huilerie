import { useCallback, useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { catalogService } from '../../services/catalogService';
import { supplierService } from '../../services/supplierService';
import { referenceService } from '../../services/referenceService';
import { purchaseService } from '../../services/purchaseService';
import { formatDT } from '../../utils/format';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';
import { todayISO } from '../../utils/format';

/** Aperçu des totaux en millimes ; le serveur refait le calcul. */
function lineTotals(l, taxes) {
  const gross = Math.round((toMillimes(l.quantite) * toMillimes(l.prixUnitaire)) / 1000);
  const ht = Math.round(gross * (1 - (Number(normalizeDecimal(l.remisePct)) || 0) / 100));
  const tax = taxes.find((t) => String(t.id) === String(l.taxId));
  const tva = tax ? Math.round((ht * Number(tax.taux)) / 100) : 0;
  return { ht, tva };
}

export default function PurchaseFormModal({ purchase, onClose, onSaved }) {
  const [supplierId, setSupplierId] = useState(purchase?.fournisseurId ?? '');
  const [date, setDate] = useState(purchase?.dateCommande ?? todayISO());
  const [refFacture, setRefFacture] = useState(purchase?.refFacture ?? '');
  const [notes, setNotes] = useState(purchase?.notes ?? '');
  const [lines, setLines] = useState(() => (purchase?.lignes ?? []).map((l) => ({
    key: l.id, articleId: l.articleId, nom: l.designation, unite: l.unite,
    quantite: String(l.quantite), prixUnitaire: String(l.prixUnitaire), remisePct: String(l.remisePct), taxId: '',
    tauxTva: l.tauxTva,
  })));
  const [taxes, setTaxes] = useState([]);
  const [picker, setPicker] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    referenceService.taxes().then((t) => {
      const active = t.filter((x) => x.actif);
      setTaxes(active);
      // retrouve la taxe des lignes existantes à partir de leur taux
      setLines((ls) => ls.map((l) => ({ ...l, taxId: active.find((x) => Number(x.taux) === Number(l.tauxTva))?.id ?? '' })));
    }).catch(() => {});
  }, []);

  const fetchSuppliers = useCallback((q) => supplierService.list({ q, actif: true, size: 50 }), []);
  const toSupplier = useCallback((s) => ({ value: s.id, label: `${s.nom} (${s.code})` }), []);
  const fetchArticles = useCallback((q) => catalogService.articles({ q, actif: true, size: 50 }), []);
  const toArticle = useCallback((a) => ({ value: a.id, label: `${a.nom} (${a.reference})` }), []);

  const addArticle = (id, a) => {
    if (!a) return;
    setLines((ls) => [...ls, {
      key: `${id}-${Date.now()}`, articleId: a.id, nom: a.nom, unite: a.uniteCode, quantite: '1',
      prixUnitaire: a.prixAchat != null ? String(a.prixAchat) : '', remisePct: '0', taxId: taxes[0]?.id ?? '',
    }]);
    setPicker('');
  };
  const update = (key, patch) => setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  const totals = lines.reduce((acc, l) => {
    const t = lineTotals(l, taxes);
    return { ht: acc.ht + t.ht, tva: acc.tva + t.tva };
  }, { ht: 0, tva: 0 });

  const valid = supplierId && date && lines.length && lines.every((l) => toMillimes(l.quantite) > 0 && l.prixUnitaire !== '');

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      const saved = await purchaseService.save(purchase?.id, {
        fournisseurId: Number(supplierId), dateCommande: date, refFacture: refFacture || null, notes: notes || null,
        lignes: lines.map((l) => ({
          articleId: l.articleId, quantite: normalizeDecimal(l.quantite), prixUnitaire: normalizeDecimal(l.prixUnitaire),
          remisePct: normalizeDecimal(l.remisePct) || '0', taxId: l.taxId ? Number(l.taxId) : null,
        })),
      });
      onSaved(saved);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title={purchase ? `Modifier ${purchase.numero}` : 'Nouvelle commande fournisseur'} onClose={onClose} wide
           footer={(
             <>
               <Button variant="secondary" onClick={onClose}>Annuler</Button>
               <Button onClick={submit} loading={saving} disabled={!valid}>Enregistrer le brouillon</Button>
             </>
           )}>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        <RemoteSelect label="Fournisseur" required value={supplierId} onChange={(id) => setSupplierId(id)}
                      fetcher={fetchSuppliers} toOption={toSupplier}
                      placeholder={purchase ? purchase.fournisseurNom : 'Choisir…'} />
        <div>
          <div className="field">
            <label>Date de commande *</label>
            <input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div className="field">
            <label>Réf. facture fournisseur</label>
            <input className="input" value={refFacture} onChange={(e) => setRefFacture(e.target.value)} maxLength={60} />
          </div>
        </div>
      </div>

      <RemoteSelect label="Ajouter un article" value={picker} onChange={addArticle} fetcher={fetchArticles} toOption={toArticle} />
      {!taxes.length ? <p className="hint muted" style={{ marginBottom: 10 }}>Aucune taxe paramétrée : les lignes sont sans TVA (Paramètres › Taxes).</p> : null}

      {lines.length ? (
        <div className="table-wrap" style={{ border: '1px solid var(--beige-300)', borderRadius: 8, marginBottom: 14 }}>
          <table className="table table-edit">
            <thead><tr><th>Article</th><th>Quantité</th><th>Prix unit. HT</th><th>Remise %</th><th>TVA</th><th className="num">Total HT</th><th /></tr></thead>
            <tbody>
              {lines.map((l) => {
                const t = lineTotals(l, taxes);
                return (
                  <tr key={l.key}>
                    <td><strong>{l.nom}</strong><div className="muted" style={{ fontSize: 12 }}>{l.unite}</div></td>
                    <td><input className="input" inputMode="decimal" value={l.quantite} onChange={(e) => update(l.key, { quantite: e.target.value })} /></td>
                    <td><input className="input" inputMode="decimal" value={l.prixUnitaire} onChange={(e) => update(l.key, { prixUnitaire: e.target.value })} /></td>
                    <td><input className="input" inputMode="decimal" value={l.remisePct} onChange={(e) => update(l.key, { remisePct: e.target.value })} /></td>
                    <td>
                      <select className="select" value={l.taxId} onChange={(e) => update(l.key, { taxId: e.target.value })}>
                        <option value="">Sans TVA</option>
                        {taxes.map((x) => <option key={x.id} value={x.id}>{x.nom}</option>)}
                      </select>
                    </td>
                    <td className="num">{formatDT(fromMillimes(t.ht))}</td>
                    <td><button type="button" className="btn btn-ghost btn-sm" aria-label="Supprimer" onClick={() => setLines((ls) => ls.filter((x) => x.key !== l.key))}><Trash2 size={16} /></button></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : <p className="muted" style={{ marginBottom: 14 }}>Ajoutez les articles commandés.</p>}

      <dl className="kv" style={{ justifyContent: 'end', marginBottom: 14 }}>
        <dt>Total HT</dt><dd>{formatDT(fromMillimes(totals.ht))}</dd>
        <dt>TVA</dt><dd>{formatDT(fromMillimes(totals.tva))}</dd>
        <dt>Total TTC</dt><dd><strong>{formatDT(fromMillimes(totals.ht + totals.tva))}</strong></dd>
      </dl>
      <div className="field">
        <label>Notes</label>
        <textarea className="input textarea" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
      </div>
    </Modal>
  );
}
