import { useCallback, useEffect, useState } from 'react';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { clientService } from '../../services/clientService';
import { Container, Pencil, Plus } from 'lucide-react';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import DataTable from '../../components/tables/DataTable';
import { tankService } from '../../services/stockService';
import { catalogService } from '../../services/catalogService';
import { formatQty } from '../../utils/format';
import { TANK_TYPES } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';

/**
 * Création ou modification d'un puits ; le code, l'huile stockée et le propriétaire sont fixés à la création.
 * Propriétaire : l'huilerie (huile achetée, vendue en caisse) ou un client (huile de ses pressages à façon).
 */
function TankModal({ tank, onClose, onSaved }) {
  const creating = !tank.id;
  const [form, setForm] = useState({ code: tank.code || '', nom: tank.nom || '', type: tank.type || 'PUITS',
    articleId: tank.articleId || '', capacite: tank.capacite != null ? String(tank.capacite) : '',
    owner: tank.clientId ? 'CLIENT' : 'HUILERIE', clientId: tank.clientId || '' });
  const fetchClients = useCallback((q) => clientService.list({ q, actif: true, size: 30 }), []);
  const toClient = useCallback((c) => ({ value: c.id, label: `${c.nom} (${c.code})` }), []);
  const [oils, setOils] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  useEffect(() => {
    if (creating) {
      catalogService.articles({ actif: true, size: 100 })
        .then((p) => setOils(p.content.filter((a) => a.suiviLot && a.type === 'HUILE_VRAC'))).catch(() => {});
    }
  }, [creating]);

  const forClient = form.owner === 'CLIENT';
  const valid = form.nom.trim() && toMillimes(form.capacite) > 0
    && (!creating || (form.code.trim() && form.articleId && (!forClient || form.clientId)));
  const save = async () => {
    setSaving(true);
    setError('');
    try {
      onSaved(await tankService.save(tank.id, {
        code: creating ? form.code.trim() : null, nom: form.nom.trim(), type: form.type,
        articleId: creating ? Number(form.articleId) : null, capacite: normalizeDecimal(form.capacite),
        clientId: creating && forClient ? Number(form.clientId) : null,
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <Modal title={creating ? 'Nouveau contenant (citerne, fût, puits…)' : `${TANK_TYPES[tank.type] || 'Contenant'} ${tank.code}`} onClose={onClose}
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={save} loading={saving} disabled={!valid}>Enregistrer</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="field">
        <label>Propriétaire *</label>
        {creating ? (
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <label className="check"><input type="radio" name="owner" value="HUILERIE" checked={!forClient} onChange={set('owner')} />L'huilerie</label>
            <label className="check"><input type="radio" name="owner" value="CLIENT" checked={forClient} onChange={set('owner')} />Un client</label>
          </div>
        ) : <input className="input" value={tank.clientNom || "L'huilerie"} disabled />}
        <span className="hint">{forClient
          ? "Réservé à ce client : l'huile de ses pressages à façon y est versée, puis lui est remise. Jamais vendue."
          : "Huile achetée par l'huilerie : versée après un achat d'olives, vendue en caisse."}</span>
      </div>
      {creating && forClient ? (
        <RemoteSelect label="Client" required value={form.clientId} onChange={(id) => setForm((f) => ({ ...f, clientId: id }))}
                      fetcher={fetchClients} toOption={toClient} />
      ) : null}
      <div className="form-grid">
        <div className="field"><label>Code *</label>
          <input className="input" value={form.code} onChange={set('code')} disabled={!creating} placeholder="Ex. PUITS-1" maxLength={30} />
          <span className="hint">Affiché en caisse comme lot ; non modifiable.</span></div>
        <div className="field"><label>Nom *</label><input className="input" value={form.nom} onChange={set('nom')} maxLength={100} placeholder="Ex. Puits 1 (sous-sol)" /></div>
        <div className="field"><label>Type *</label>
          <select className="select" value={form.type} onChange={set('type')}>
            {Object.entries(TANK_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Huile stockée *</label>
          {creating ? (
            <select className="select" value={form.articleId} onChange={set('articleId')}>
              <option value="">Choisir…</option>
              {oils.map((a) => <option key={a.id} value={a.id}>{a.nom} ({a.uniteCode}){a.vendable ? '' : ' — non vendable'}</option>)}
            </select>
          ) : <input className="input" value={tank.articleNom} disabled />}
          <span className="hint">{forClient ? 'En général l\'article « Huile des clients » (non vendable), le même que sur ses bulletins.'
            : 'Article « huile en vrac » suivi par lot ; cochez « Vendable en caisse » pour le vendre.'}</span></div>
        <div className="field"><label>Capacité{tank.unite ? ` (${tank.unite})` : ''} *</label>
          <input className="input" inputMode="decimal" value={form.capacite} onChange={set('capacite')} /></div>
      </div>
    </Modal>
  );
}

export default function TanksTab({ canAdjust, reloadKey }) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);

  const load = useCallback(() => tankService.list().then(setRows).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load, reloadKey]);

  const toggle = async (t) => {
    try { await tankService.setStatus(t.id, !t.actif); load(); } catch (e) { setError(e.message); }
  };

  return (
    <div className="panel">
      {canAdjust ? (
        <div className="panel-body" style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button onClick={() => setEditing({})}><Plus size={18} />Nouvelle citerne / fût / puits</Button>
        </div>
      ) : null}
      <Alert type="error">{error}</Alert>
      {!rows ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={rows}
                   empty={<EmptyState icon={Container} title="Aucun contenant"
                                      text="Créez vos puits et silos (huile achetée, vendue en caisse) et les citernes ou fûts des clients (leur huile conservée à l'huilerie)." />}
                   columns={[
                     { key: 'code', header: 'Code', render: (t) => <><strong>{t.code}</strong><div className="muted" style={{ fontSize: 12 }}>{t.nom}</div></> },
                     { key: 'type', header: 'Type', render: (t) => TANK_TYPES[t.type] },
                     { key: 'art', header: 'Huile', render: (t) => t.articleNom },
                     { key: 'own', header: 'Propriétaire', render: (t) => (t.clientNom ? <strong>{t.clientNom}</strong> : <span className="muted">L'huilerie</span>) },
                     {
                       key: 'q', header: 'Contenu', align: 'right',
                       render: (t) => {
                         const pct = Number(t.capacite) ? Math.round((Number(t.quantite) * 100) / Number(t.capacite)) : 0;
                         return <><strong>{formatQty(t.quantite, t.unite)}</strong> <span className="muted">/ {formatQty(t.capacite, t.unite)} ({pct} %)</span></>;
                       },
                     },
                     { key: 'p', header: 'Place restante', align: 'right', render: (t) => formatQty(t.placeRestante, t.unite) },
                     { key: 'b', header: 'Vente', render: (t) => (t.bloque ? <span className="badge badge-off">Lot bloqué</span>
                       : t.clientId ? <span className="badge badge-role">Huile du client</span> : <span className="badge badge-ok">Vendable</span>) },
                     { key: 's', header: 'Statut', render: (t) => <StatusBadge active={t.actif} /> },
                     ...(canAdjust ? [{
                       key: 'a', header: '', align: 'right',
                       render: (t) => (
                         <div className="actions" onClick={(e) => e.stopPropagation()}>
                           <Button variant="secondary" onClick={() => setEditing(t)}><Pencil size={14} />Modifier</Button>
                           <Button variant="secondary" onClick={() => toggle(t)}>{t.actif ? 'Désactiver' : 'Réactiver'}</Button>
                         </div>
                       ),
                     }] : []),
                   ]} />
      )}
      {editing ? <TankModal tank={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); load(); }} /> : null}
    </div>
  );
}
