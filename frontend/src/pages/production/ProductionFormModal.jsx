import { useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import { catalogService } from '../../services/catalogService';
import { stockService } from '../../services/stockService';
import { productionService } from '../../services/operationsService';
import { formatQty, todayISO } from '../../utils/format';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

/** Planification d'une trituration : lots d'olives de l'huilerie à consommer et huile à produire. */
export default function ProductionFormModal({ order, onClose, onSaved }) {
  const [date, setDate] = useState(order?.datePrevue ?? todayISO());
  const [ligne, setLigne] = useState(order?.ligne ?? '');
  const [notes, setNotes] = useState(order?.notes ?? '');
  const [huileId, setHuileId] = useState(order?.huileArticleId ?? '');
  const [oils, setOils] = useState([]);
  const [olives, setOlives] = useState([]);       // stock d'olives disponible par lot
  const [inputs, setInputs] = useState([]);       // { key, articleId, lotId, label, dispo, quantite }
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    catalogService.articles({ actif: true, type: 'HUILE_VRAC', size: 100 }).then((p) => setOils(p.content.filter((a) => a.suiviLot))).catch(() => {});
    stockService.state({ type: 'OLIVES', size: 100 }).then((p) => {
      const rows = [];
      p.content.forEach((a) => {
        a.lots.filter((l) => l.proprietaire === 'ENTREPRISE').forEach((l) => rows.push({
          articleId: a.articleId, lotId: l.lotId, label: `${a.nom} · lot ${l.numero}`, dispo: Number(l.quantite), unite: a.unite,
        }));
        if (!a.lots.length && Number(a.disponible) > 0) rows.push({ articleId: a.articleId, lotId: null, label: a.nom, dispo: Number(a.disponible), unite: a.unite });
      });
      setOlives(rows);
      if (order) {
        setInputs(order.entrees.map((e, i) => {
          const match = rows.find((r) => r.articleId === e.articleId && (r.lotId ?? null) === (e.lotId ?? null));
          return { key: i, articleId: e.articleId, lotId: e.lotId ?? null, label: `${e.article}${e.lot ? ` · lot ${e.lot}` : ''}`, dispo: match?.dispo ?? 0, unite: e.unite, quantite: String(e.quantite) };
        }));
      }
    }).catch((e) => setError(e.message));
  }, [order]);

  const add = (idx) => {
    const o = olives[idx];
    if (!o || inputs.some((i) => i.articleId === o.articleId && i.lotId === o.lotId)) return;
    setInputs([...inputs, { ...o, key: `${o.articleId}-${o.lotId}-${Date.now()}`, quantite: String(o.dispo) }]);
  };
  const totalM = inputs.reduce((s, i) => s + toMillimes(i.quantite), 0);
  const valid = huileId && inputs.length && inputs.every((i) => toMillimes(i.quantite) > 0);

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      onSaved(await productionService.save(order?.id, {
        datePrevue: date, ligne: ligne || null, huileArticleId: Number(huileId), notes: notes || null,
        entrees: inputs.map((i) => ({ articleId: i.articleId, lotId: i.lotId, quantite: normalizeDecimal(i.quantite) })),
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title={order ? `Modifier ${order.numero}` : 'Nouvel ordre de trituration'} onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={submit} loading={saving} disabled={!valid}>Planifier</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        <div className="field"><label>Date prévue *</label><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></div>
        <div className="field"><label>Ligne / machine</label><input className="input" value={ligne} onChange={(e) => setLigne(e.target.value)} placeholder="Ex. Ligne 1" /></div>
        <div className="field wide"><label>Huile produite (article en vrac suivi par lot) *</label>
          <select className="select" value={huileId} onChange={(e) => setHuileId(e.target.value)}>
            <option value="">Choisir…</option>
            {oils.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode})</option>)}
          </select></div>
      </div>
      <div className="field">
        <label>Ajouter un lot d'olives de l'huilerie</label>
        <select className="select" value="" onChange={(e) => add(Number(e.target.value))}>
          <option value="">{olives.length ? 'Choisir un lot…' : 'Aucune olive en stock (réceptionnez un achat d\'olives)'}</option>
          {olives.map((o, i) => <option key={`${o.articleId}-${o.lotId}`} value={i}>{o.label} · {formatQty(o.dispo, o.unite)}</option>)}
        </select>
      </div>
      {inputs.length ? (
        <table className="table table-edit" style={{ marginBottom: 12 }}>
          <thead><tr><th>Olives</th><th className="num">Disponible</th><th>Quantité à triturer</th><th /></tr></thead>
          <tbody>{inputs.map((i) => (
            <tr key={i.key}>
              <td>{i.label}</td><td className="num">{formatQty(i.dispo, i.unite)}</td>
              <td><input className="input" inputMode="decimal" value={i.quantite}
                         onChange={(e) => setInputs(inputs.map((x) => (x.key === i.key ? { ...x, quantite: e.target.value } : x)))} /></td>
              <td><button type="button" className="btn btn-ghost btn-sm" aria-label="Retirer" onClick={() => setInputs(inputs.filter((x) => x.key !== i.key))}><Trash2 size={16} /></button></td>
            </tr>
          ))}</tbody>
        </table>
      ) : null}
      <div className="weigh-summary" style={{ marginBottom: 14 }}>
        <div><span>Total d'olives</span><strong>{formatQty(fromMillimes(totalM), 'kg')}</strong></div>
        <div><span>Le stock d'olives sort au démarrage</span><strong style={{ fontSize: '1rem' }}>{inputs.length} lot{inputs.length > 1 ? 's' : ''}</strong></div>
      </div>
      <div className="field"><label>Notes</label><input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
    </Modal>
  );
}
