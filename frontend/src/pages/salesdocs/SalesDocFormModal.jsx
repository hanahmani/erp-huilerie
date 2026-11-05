import { useCallback, useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { catalogService } from '../../services/catalogService';
import { clientService } from '../../services/clientService';
import { referenceService } from '../../services/referenceService';
import { stockService } from '../../services/stockService';
import { salesDocService } from '../../services/salesDocService';
import { settingsService } from '../../services/settingsService';
import { formatDT, formatQty, todayISO } from '../../utils/format';
import { DOC_TYPES } from '../../utils/labels';
import { fromMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

function preview(l, taxes) {
  const gross = Math.round((toMillimes(l.quantite) * toMillimes(l.prixUnitaire)) / 1000);
  const ht = Math.round(gross * (1 - (Number(normalizeDecimal(l.remisePct)) || 0) / 100));
  const tax = taxes.find((t) => String(t.id) === String(l.taxId));
  return { ht, tva: tax ? Math.round((ht * Number(tax.taux)) / 100) : 0 };
}

/** Prix du catalogue mémorisés sur la ligne : l'huile au prix fixé au litre peut passer du kg au litre. */
const prices = (a) => ({ uniteStock: a.uniteCode, prixKg: a.prixVente, prixLitre: a.prixVenteLitre });

export default function SalesDocFormModal({ type, doc, onClose, onSaved }) {
  const [clientId, setClientId] = useState(doc?.clientId ?? '');
  const [date, setDate] = useState(doc?.dateDoc ?? todayISO());
  const [echeance, setEcheance] = useState(doc?.echeance ?? '');
  const [refClient, setRefClient] = useState(doc?.refClient ?? '');
  const [notes, setNotes] = useState(doc?.notes ?? '');
  const [taxes, setTaxes] = useState([]);
  const [lines, setLines] = useState(() => (doc?.lignes ?? []).map((l) => ({
    key: l.id, articleId: l.articleId, nom: l.designation, unite: l.unite, enLitres: l.enLitres, uniteStock: l.uniteStock,
    prixKg: null, prixLitre: null, suiviLot: Boolean(l.lotId), lotId: l.lotId ?? '', lots: [],
    quantite: String(l.quantite), prixUnitaire: String(l.prixUnitaire), remisePct: String(l.remisePct), tauxTva: l.tauxTva, taxId: '',
  })));
  const [picker, setPicker] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [density, setDensity] = useState(null);

  useEffect(() => { settingsService.get().then((s) => setDensity(Number(s.densiteHuile))).catch(() => {}); }, []);
  // Brouillon rouvert : on relit les prix des articles pour permettre le choix kg / litre.
  useEffect(() => {
    const ids = [...new Set((doc?.lignes ?? []).map((l) => l.articleId))];
    Promise.all(ids.map((id) => catalogService.article(id).catch(() => null))).then((arts) => {
      const byId = Object.fromEntries(arts.filter(Boolean).map((a) => [a.id, a]));
      setLines((ls) => ls.map((l) => (byId[l.articleId] ? { ...l, ...prices(byId[l.articleId]) } : l)));
    });
  }, [doc]);

  useEffect(() => {
    referenceService.taxes().then((t) => {
      const act = t.filter((x) => x.actif);
      setTaxes(act);
      setLines((ls) => ls.map((l) => ({ ...l, taxId: act.find((x) => Number(x.taux) === Number(l.tauxTva))?.id ?? '' })));
    }).catch(() => {});
  }, []);

  const fetchClients = useCallback((q) => clientService.list({ q, actif: true, size: 30 }), []);
  const toClient = useCallback((c) => ({ value: c.id, label: `${c.nom} (${c.code})` }), []);
  const fetchArticles = useCallback((q) => catalogService.articles({ q, actif: true, size: 50 }), []);
  const toArticle = useCallback((a) => ({ value: a.id, label: `${a.nom} (${a.reference})${a.prixVenteLitre != null
    ? ` · ${Number(a.prixVenteLitre).toFixed(3)} DT / L` : a.prixVente != null ? ` · ${Number(a.prixVente).toFixed(3)} DT` : ''}` }), []);

  const addArticle = async (id, a) => {
    if (!a) return;
    let lots = [];
    if (a.suiviLot) {
      lots = (await stockService.lots({ articleId: a.id, owner: 'ENTREPRISE', statut: 'DISPONIBLE', size: 50 }).catch(() => ({ content: [] }))).content
        .filter((l) => Number(l.quantite) > 0);
    }
    // Huile au prix fixé au litre : ligne au litre par défaut, comme en caisse.
    const enLitres = a.prixVenteLitre != null;
    const prix = enLitres ? a.prixVenteLitre : a.prixVente;
    setLines((ls) => [...ls, {
      key: `${id}-${Date.now()}`, articleId: a.id, nom: a.nom, unite: enLitres ? 'L' : a.uniteCode, enLitres, ...prices(a),
      suiviLot: a.suiviLot, lots, lotId: lots[0]?.id ?? '',
      quantite: '1', prixUnitaire: prix != null ? String(prix) : '', remisePct: '0', taxId: taxes[0]?.id ?? '',
    }]);
    setPicker('');
  };
  const update = (key, patch) => setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));
  const setMode = (l, enLitres) => {
    const prix = enLitres ? l.prixLitre : l.prixKg;
    update(l.key, { enLitres, unite: enLitres ? 'L' : l.uniteStock, prixUnitaire: prix != null ? String(prix) : l.prixUnitaire });
  };
  const kgOf = (l) => (density ? fromMillimes(Math.round(toMillimes(l.quantite) * density)) : null);

  const totals = lines.reduce((acc, l) => { const t = preview(l, taxes); return { ht: acc.ht + t.ht, tva: acc.tva + t.tva }; }, { ht: 0, tva: 0 });
  const needsLot = type === 'LIVRAISON' || type === 'FACTURE';
  const valid = clientId && date && lines.length && lines.every((l) => toMillimes(l.quantite) > 0 && l.prixUnitaire !== '' && (!needsLot || !l.suiviLot || l.lotId));

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      onSaved(await salesDocService.save(doc?.id, {
        type, clientId: Number(clientId), dateDoc: date, echeance: echeance || null, refClient: refClient || null, notes: notes || null,
        lignes: lines.map((l) => ({
          articleId: l.articleId, lotId: l.lotId ? Number(l.lotId) : null, quantite: normalizeDecimal(l.quantite),
          prixUnitaire: normalizeDecimal(l.prixUnitaire), remisePct: normalizeDecimal(l.remisePct) || '0', taxId: l.taxId ? Number(l.taxId) : null,
          enLitres: Boolean(l.enLitres),
        })),
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title={doc ? `Modifier le brouillon` : `Nouveau : ${DOC_TYPES[type].toLowerCase()}`} onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button><Button onClick={submit} loading={saving} disabled={!valid}>Enregistrer le brouillon</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        <RemoteSelect label="Client" required value={clientId} onChange={(id) => setClientId(id)} fetcher={fetchClients} toOption={toClient}
                      placeholder={doc ? doc.clientNom : 'Choisir…'} />
        <div>
          <div className="field"><label>Date *</label><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></div>
          {type === 'FACTURE' ? <div className="field"><label>Échéance</label><input type="date" className="input" value={echeance} onChange={(e) => setEcheance(e.target.value)} /></div> : null}
          <div className="field"><label>Référence client</label><input className="input" value={refClient} onChange={(e) => setRefClient(e.target.value)} /></div>
        </div>
      </div>
      <RemoteSelect label="Ajouter un article" value={picker} onChange={addArticle} fetcher={fetchArticles} toOption={toArticle} />
      {lines.length ? (
        <div className="table-wrap" style={{ border: '1px solid var(--beige-300)', borderRadius: 8, marginBottom: 14 }}>
          <table className="table table-edit">
            <thead><tr><th>Article</th><th>Lot</th><th>Qté</th><th>P.U. HT</th><th>Rem. %</th><th>TVA</th><th className="num">Total HT</th><th /></tr></thead>
            <tbody>{lines.map((l) => (
              <tr key={l.key}>
                <td><strong>{l.nom}</strong>
                  {l.prixLitre != null || l.enLitres ? (
                    <div className="pos-unit" role="group" aria-label="Unité de vente">
                      <button type="button" className={l.enLitres ? 'active' : ''} disabled={l.prixLitre == null} onClick={() => setMode(l, true)}>Litre</button>
                      <button type="button" className={l.enLitres ? '' : 'active'} onClick={() => setMode(l, false)}>{l.uniteStock}</button>
                    </div>
                  ) : <div className="muted" style={{ fontSize: 12 }}>{l.unite}</div>}
                  {l.enLitres && toMillimes(l.quantite) > 0 && kgOf(l)
                    ? <div className="muted" style={{ fontSize: 12 }}>= {formatQty(kgOf(l), l.uniteStock)} sortis du stock</div> : null}
                </td>
                <td>{l.suiviLot ? (
                  <select className="select" value={l.lotId} onChange={(e) => update(l.key, { lotId: e.target.value })}>
                    <option value="">Choisir…</option>
                    {l.lots.map((x) => <option key={x.id} value={x.id}>{x.numero} · {formatQty(x.quantite)}</option>)}
                    {l.lotId && !l.lots.some((x) => String(x.id) === String(l.lotId)) ? <option value={l.lotId}>Lot actuel</option> : null}
                  </select>) : <span className="muted">—</span>}</td>
                <td><input className="input" inputMode="decimal" value={l.quantite} onChange={(e) => update(l.key, { quantite: e.target.value })} /></td>
                <td><input className="input" inputMode="decimal" value={l.prixUnitaire} onChange={(e) => update(l.key, { prixUnitaire: e.target.value })} /></td>
                <td><input className="input" inputMode="decimal" value={l.remisePct} onChange={(e) => update(l.key, { remisePct: e.target.value })} /></td>
                <td><select className="select" value={l.taxId} onChange={(e) => update(l.key, { taxId: e.target.value })}>
                  <option value="">Sans TVA</option>{taxes.map((x) => <option key={x.id} value={x.id}>{x.nom}</option>)}</select></td>
                <td className="num">{formatDT(fromMillimes(preview(l, taxes).ht))}</td>
                <td><button type="button" className="btn btn-ghost btn-sm" onClick={() => setLines(lines.filter((x) => x.key !== l.key))}><Trash2 size={16} /></button></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      ) : <p className="muted" style={{ marginBottom: 14 }}>Ajoutez les articles.</p>}
      <dl className="kv" style={{ justifyContent: 'end', marginBottom: 14 }}>
        <dt>Total HT</dt><dd>{formatDT(fromMillimes(totals.ht))}</dd>
        <dt>TVA</dt><dd>{formatDT(fromMillimes(totals.tva))}</dd>
        <dt>Total TTC</dt><dd><strong>{formatDT(fromMillimes(totals.ht + totals.tva))}</strong>{type === 'FACTURE' ? <span className="muted"> + timbre à la validation</span> : null}</dd>
      </dl>
      <div className="field"><label>Notes</label><textarea className="input textarea" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
    </Modal>
  );
}
