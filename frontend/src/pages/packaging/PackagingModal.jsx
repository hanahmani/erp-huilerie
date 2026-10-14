import { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import { catalogService } from '../../services/catalogService';
import { stockService } from '../../services/stockService';
import { packagingService } from '../../services/operationsService';
import { formatQty } from '../../utils/format';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

/** Conditionnement : vrac de l'huilerie → bouteilles ou bidons, en une seule opération. */
export default function PackagingModal({ onClose, onDone }) {
  const [sources, setSources] = useState([]);      // { key, articleId, lotId, label, dispo, unite }
  const [products, setProducts] = useState([]);
  const [supplies, setSupplies] = useState([]);
  const [sourceKey, setSourceKey] = useState('');
  const [productId, setProductId] = useState('');
  const [qty, setQty] = useState('');
  const [cons, setCons] = useState([]);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    stockService.state({ type: 'HUILE_VRAC', size: 100 }).then((p) => {
      const rows = [];
      p.content.forEach((a) => {
        a.lots.filter((l) => l.proprietaire === 'ENTREPRISE' && l.statut === 'DISPONIBLE')
          .forEach((l) => rows.push({ key: `${a.articleId}-${l.lotId}`, articleId: a.articleId, lotId: l.lotId, label: `${a.nom} · lot ${l.numero}`, dispo: Number(l.quantite), unite: a.unite }));
        if (!a.lots.length && Number(a.disponible) > 0) rows.push({ key: `${a.articleId}-0`, articleId: a.articleId, lotId: null, label: a.nom, dispo: Number(a.disponible), unite: a.unite });
      });
      setSources(rows);
    }).catch((e) => setError(e.message));
    catalogService.articles({ actif: true, type: 'HUILE_CONDITIONNEE', size: 100 }).then((p) => setProducts(p.content)).catch(() => {});
    Promise.all([catalogService.articles({ actif: true, type: 'EMBALLAGE', size: 100 }), catalogService.articles({ actif: true, type: 'CONSOMMABLE', size: 100 })])
      .then(([a, b]) => setSupplies([...a.content, ...b.content].filter((x) => !x.suiviLot))).catch(() => {});
  }, []);

  const source = sources.find((s) => s.key === sourceKey);
  const product = products.find((p) => String(p.id) === String(productId));
  const volumeM = product?.contenance ? Math.round((toMillimes(qty) * toMillimes(product.contenance)) / 1000) : 0;
  const tooMuch = source && volumeM > Math.round(source.dispo * 1000);
  const valid = source && product && product.contenance && toMillimes(qty) > 0 && !tooMuch && cons.every((c) => c.articleId && toMillimes(c.quantite) > 0);

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      onDone(await packagingService.create({
        sourceArticleId: source.articleId, sourceLotId: source.lotId, produitArticleId: product.id,
        quantiteProduite: normalizeDecimal(qty), notes: notes || null,
        consommations: cons.map((c) => ({ articleId: Number(c.articleId), quantite: normalizeDecimal(c.quantite) })),
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title="Nouveau conditionnement" onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={submit} loading={saving} disabled={!valid}>Valider le conditionnement</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        <div className="field wide"><label>Huile en vrac utilisée *</label>
          <select className="select" value={sourceKey} onChange={(e) => setSourceKey(e.target.value)}>
            <option value="">{sources.length ? 'Choisir un lot de vrac…' : 'Aucun vrac disponible (lots de l\'huilerie non bloqués)'}</option>
            {sources.map((s) => <option key={s.key} value={s.key}>{s.label} · {formatQty(s.dispo, s.unite)}</option>)}
          </select>
          <span className="hint">L'huile des clients et les lots bloqués ne sont jamais proposés.</span></div>
        <div className="field"><label>Produit fini *</label>
          <select className="select" value={productId} onChange={(e) => setProductId(e.target.value)}>
            <option value="">Choisir…</option>
            {products.map((p) => <option key={p.id} value={p.id}>{p.nom}{p.contenance ? ` (${Number(p.contenance)} L)` : ' (contenance manquante)'}</option>)}
          </select>
          {product && !product.contenance ? <span className="error">Indiquez la contenance de cet article dans le catalogue.</span> : null}</div>
        <div className="field"><label>Quantité produite ({product?.uniteCode || 'unités'}) *</label>
          <input className="input" inputMode="decimal" value={qty} onChange={(e) => setQty(e.target.value)} /></div>
      </div>
      <div className="weigh-summary" style={{ marginBottom: 14 }}>
        <div><span>Volume de vrac utilisé</span><strong>{volumeM ? formatQty(fromMillimes(volumeM), source?.unite || 'L') : '—'}</strong></div>
        <div><span>Restera en vrac</span><strong style={tooMuch ? { color: 'var(--danger)' } : undefined}>
          {source ? (tooMuch ? 'Insuffisant' : formatQty(fromMillimes(Math.round(source.dispo * 1000) - volumeM), source.unite)) : '—'}</strong></div>
      </div>
      <div className="field"><label>Emballages et consommables utilisés</label>
        {cons.map((c, i) => (
          <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
            <select className="select" value={c.articleId} onChange={(e) => setCons(cons.map((x, j) => (j === i ? { ...x, articleId: e.target.value } : x)))}>
              <option value="">Article…</option>
              {supplies.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode})</option>)}
            </select>
            <input className="input" inputMode="decimal" style={{ width: 140 }} placeholder="Quantité" value={c.quantite}
                   onChange={(e) => setCons(cons.map((x, j) => (j === i ? { ...x, quantite: e.target.value } : x)))} />
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setCons(cons.filter((_, j) => j !== i))}><Trash2 size={16} /></button>
          </div>
        ))}
        <Button size="sm" variant="secondary" disabled={!supplies.length}
                onClick={() => setCons([...cons, { articleId: '', quantite: qty || '' }])}><Plus size={14} />Ajouter un emballage</Button>
        {!supplies.length ? <span className="hint">Créez des articles de type « Emballage » (bouteilles vides, bouchons, étiquettes…).</span> : null}
      </div>
      <div className="field"><label>Notes</label><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
    </Modal>
  );
}
