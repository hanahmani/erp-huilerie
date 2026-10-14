import { useEffect, useState } from 'react';
import { Ban, CheckCircle2, Pencil, Play, Plus, Trash2 } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import { useAuth } from '../../context/AuthContext';
import { catalogService } from '../../services/catalogService';
import { productionService } from '../../services/operationsService';
import { formatDateTime, formatQty } from '../../utils/format';
import { OIL_CATEGORIES, PRODUCTION_STATUS, PRODUCTION_STATUS_CLASS } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';
import ProductionFormModal from './ProductionFormModal';

export default function ProductionDetailModal({ id, onClose, onChanged }) {
  const { can } = useAuth();
  const manage = can('PRODUCTION_MANAGE');
  const [o, setO] = useState(null);
  const [panel, setPanel] = useState(null);
  const [finish, setFinish] = useState({ huile: '', pertes: '', acidite: '', lot: '', bloquer: true, sous: [] });
  const [byArticles, setByArticles] = useState([]);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { productionService.get(id).then(setO).catch((e) => setError(e.message)); }, [id]);
  useEffect(() => {
    if (panel === 'finish') {
      catalogService.articles({ actif: true, type: 'SOUS_PRODUIT', size: 50 }).then((p) => setByArticles(p.content.filter((a) => !a.suiviLot))).catch(() => {});
    }
  }, [panel]);

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try { setO(await fn()); setPanel(null); onChanged?.(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };

  if (panel === 'edit') return <ProductionFormModal order={o} onClose={() => setPanel(null)} onSaved={(x) => { setO(x); setPanel(null); onChanged?.(); }} />;

  const yieldPreview = toMillimes(finish.huile) && o ? ((toMillimes(finish.huile) / toMillimes(o.olivesKg)) * 100).toFixed(2) : null;

  return (
    <Modal title={o ? `Trituration ${o.numero}` : 'Trituration'} onClose={onClose} wide
           footer={o && manage ? (
             <>
               {o.statut === 'PLANIFIE' ? <Button variant="danger" onClick={() => setPanel('cancel')}><Ban size={16} />Annuler</Button> : null}
               {o.statut === 'PLANIFIE' ? <Button variant="secondary" onClick={() => setPanel('edit')}><Pencil size={16} />Modifier</Button> : null}
               {o.statut === 'PLANIFIE' ? <Button loading={busy} onClick={() => run(() => productionService.start(o.id))}><Play size={16} />Démarrer (consomme les olives)</Button> : null}
               {o.statut === 'EN_COURS' ? <Button onClick={() => setPanel('finish')}><CheckCircle2 size={16} />Terminer</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!o ? <Spinner /> : (
        <>
          {o.statut === 'ANNULE' ? <Alert type="error">Annulé : {o.motifAnnulation}</Alert> : null}
          <div className="grid-2">
            <dl className="kv">
              <dt>Statut</dt><dd><span className={`badge ${PRODUCTION_STATUS_CLASS[o.statut]}`}>{PRODUCTION_STATUS[o.statut]}</span></dd>
              <dt>Date prévue</dt><dd>{new Date(`${o.datePrevue}T12:00:00`).toLocaleDateString('fr-FR')}</dd>
              <dt>Ligne</dt><dd>{o.ligne || '—'}</dd>
              <dt>Huile produite</dt><dd>{o.huileArticle}</dd>
              <dt>Créé par</dt><dd>{o.creePar}, le {formatDateTime(o.createdAt)}</dd>
              {o.demarreLe ? <><dt>Démarré</dt><dd>{formatDateTime(o.demarreLe)}</dd></> : null}
              {o.termineLe ? <><dt>Terminé</dt><dd>{formatDateTime(o.termineLe)} par {o.terminePar}</dd></> : null}
            </dl>
            <dl className="kv">
              <dt>Olives</dt><dd><strong>{formatQty(o.olivesKg, 'kg')}</strong></dd>
              <dt>Huile obtenue</dt><dd>{o.huileLitres != null ? <strong>{formatQty(o.huileLitres, o.huileUnite)}</strong> : '—'}</dd>
              <dt>Rendement</dt><dd>{o.rendement != null ? `${Number(o.rendement).toFixed(2)} ${o.huileUnite} / 100 kg` : '—'}</dd>
              <dt>Pertes</dt><dd>{o.pertesKg != null ? formatQty(o.pertesKg, 'kg') : '—'}</dd>
              <dt>Acidité</dt><dd>{o.acidite != null ? `${Number(o.acidite)} %` : '—'}</dd>
              <dt>Catégorie</dt><dd>{o.categorie ? <span className="badge badge-role">{OIL_CATEGORIES[o.categorie]}</span> : '—'}</dd>
              <dt>Lot d'huile</dt><dd>{o.huileLot || '—'}</dd>
            </dl>
          </div>
          <div className="grid-2" style={{ marginTop: 16 }}>
            <div><h2 style={{ marginBottom: 8 }}>Olives consommées</h2>
              <table className="table"><tbody>{o.entrees.map((e, i) => (
                <tr key={i}><td>{e.article}{e.lot ? <span className="muted"> · lot {e.lot}</span> : null}</td><td className="num">{formatQty(e.quantite, e.unite)}</td></tr>
              ))}</tbody></table></div>
            <div><h2 style={{ marginBottom: 8 }}>Sous-produits</h2>
              {o.sousProduits.length ? <table className="table"><tbody>{o.sousProduits.map((b, i) => (
                <tr key={i}><td>{b.article}</td><td className="num">{formatQty(b.quantite, b.unite)}</td></tr>
              ))}</tbody></table> : <p className="muted">Aucun.</p>}</div>
          </div>

          {panel === 'finish' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              <h2 style={{ marginBottom: 12 }}>Fin de la trituration</h2>
              <div className="form-grid">
                <div className="field"><label>Huile obtenue ({o.huileUnite}) *</label>
                  <input className="input" inputMode="decimal" autoFocus value={finish.huile} onChange={(e) => setFinish({ ...finish, huile: e.target.value })} />
                  {yieldPreview ? <span className="hint">Rendement : {yieldPreview} {o.huileUnite} / 100 kg</span> : null}</div>
                <div className="field"><label>Pertes (kg)</label>
                  <input className="input" inputMode="decimal" value={finish.pertes} onChange={(e) => setFinish({ ...finish, pertes: e.target.value })} /></div>
                <div className="field"><label>Acidité libre (%)</label>
                  <input className="input" inputMode="decimal" value={finish.acidite} onChange={(e) => setFinish({ ...finish, acidite: e.target.value })} />
                  <span className="hint">Classement indicatif : ≤ 0,8 extra vierge · ≤ 2 vierge · ≤ 3,3 courante · au-delà lampante.</span></div>
                <div className="field"><label>Numéro du lot d'huile</label>
                  <input className="input" value={finish.lot} placeholder={o.numero} onChange={(e) => setFinish({ ...finish, lot: e.target.value })} /></div>
                <label className="check check-field wide"><input type="checkbox" checked={finish.bloquer} onChange={(e) => setFinish({ ...finish, bloquer: e.target.checked })} />
                  <span>Bloquer le lot jusqu'à l'analyse qualité<small>Il ne pourra être vendu ou conditionné qu'après déblocage dans Stock › Lots.</small></span></label>
              </div>
              <div className="field"><label>Sous-produits (grignons, margines…)</label>
                {finish.sous.map((s, i) => (
                  <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                    <select className="select" value={s.articleId} onChange={(e) => setFinish({ ...finish, sous: finish.sous.map((x, j) => (j === i ? { ...x, articleId: e.target.value } : x)) })}>
                      <option value="">Article…</option>
                      {byArticles.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode})</option>)}
                    </select>
                    <input className="input" inputMode="decimal" placeholder="Quantité" style={{ width: 140 }} value={s.quantite}
                           onChange={(e) => setFinish({ ...finish, sous: finish.sous.map((x, j) => (j === i ? { ...x, quantite: e.target.value } : x)) })} />
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setFinish({ ...finish, sous: finish.sous.filter((_, j) => j !== i) })}><Trash2 size={16} /></button>
                  </div>
                ))}
                <Button size="sm" variant="secondary" disabled={!byArticles.length} onClick={() => setFinish({ ...finish, sous: [...finish.sous, { articleId: '', quantite: '' }] })}>
                  <Plus size={14} />Ajouter un sous-produit</Button>
                {!byArticles.length ? <span className="hint">Créez des articles de type « Sous-produit » sans suivi par lot dans le catalogue.</span> : null}
              </div>
              <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Fermer</Button>
                <Button loading={busy} disabled={!toMillimes(finish.huile) || finish.sous.some((s) => !s.articleId || !toMillimes(s.quantite))}
                        onClick={() => run(() => productionService.finish(o.id, {
                          huileLitres: normalizeDecimal(finish.huile), pertesKg: finish.pertes ? normalizeDecimal(finish.pertes) : null,
                          acidite: finish.acidite ? normalizeDecimal(finish.acidite) : null, lotNumero: finish.lot || null, bloquerLot: finish.bloquer,
                          sousProduits: finish.sous.map((s) => ({ articleId: Number(s.articleId), quantite: normalizeDecimal(s.quantite) })),
                        }))}>Enregistrer la production</Button></div>
            </div>
          ) : null}
          {panel === 'cancel' ? (
            <div className="panel" style={{ padding: 16, marginTop: 16 }}>
              <div className="field"><label>Motif d'annulation *</label><input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} autoFocus /></div>
              <div className="actions"><Button variant="secondary" onClick={() => setPanel(null)}>Ne pas annuler</Button>
                <Button variant="danger" disabled={!motif.trim()} loading={busy} onClick={() => run(() => productionService.cancel(o.id, motif.trim()))}>Confirmer</Button></div>
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}
