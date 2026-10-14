import { useEffect, useState } from 'react';
import { ClipboardList, Download, Plus } from 'lucide-react';
import { usePagedList } from '../../hooks/usePagedList';
import { inventoryService } from '../../services/stockService';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { formatDateTime, formatQty } from '../../utils/format';
import { INVENTORY_STATUS } from '../../utils/labels';
import { normalizeDecimal } from '../../utils/money';
import { exportCsv } from '../../utils/exportCsv';

function InventoryModal({ id, canAdjust, onClose, onChanged }) {
  const [inv, setInv] = useState(null);
  const [counts, setCounts] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(null);

  const load = (data) => {
    setInv(data);
    setCounts(Object.fromEntries(data.lignes.map((l) => [l.id, l.comptee != null ? String(l.comptee) : ''])));
  };
  useEffect(() => { inventoryService.get(id).then(load).catch((e) => setError(e.message)); }, [id]);

  const open = inv?.entete.statut === 'EN_COURS' && canAdjust;
  const save = async () => {
    setBusy(true);
    setError('');
    try {
      load(await inventoryService.count(id, inv.lignes.map((l) => ({
        lineId: l.id, quantiteComptee: counts[l.id] === '' ? null : normalizeDecimal(counts[l.id]),
      }))));
      onChanged();
      return true;
    } catch (e) {
      setError(e.message);
      return false;
    } finally {
      setBusy(false);
    }
  };
  const finish = async (action) => {
    setConfirm(null);
    if (action === 'validate' && !(await save())) return;
    setBusy(true);
    try {
      load(action === 'validate' ? await inventoryService.validate(id) : await inventoryService.cancel(id));
      onChanged();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={inv ? `Inventaire ${inv.entete.numero}` : 'Inventaire'} onClose={onClose} wide
           footer={inv ? (
             <>
               <Button variant="secondary" onClick={() => exportCsv(`inventaire-${inv.entete.numero}`, [
                 { header: 'Référence', value: (l) => l.reference }, { header: 'Article', value: (l) => l.article },
                 { header: 'Lot', value: (l) => l.lot }, { header: 'Unité', value: (l) => l.unite },
                 { header: 'Théorique', value: (l) => Number(l.theorique) },
                 { header: 'Compté', value: (l) => (l.comptee == null ? '' : Number(l.comptee)) },
                 { header: 'Écart', value: (l) => (l.ecart == null ? '' : Number(l.ecart)) },
               ], inv.lignes)}><Download size={16} />Exporter</Button>
               {open ? <Button variant="danger" onClick={() => setConfirm('cancel')}>Annuler l'inventaire</Button> : null}
               {open ? <Button variant="secondary" loading={busy} onClick={save}>Enregistrer le comptage</Button> : null}
               {open ? <Button onClick={() => setConfirm('validate')}>Valider l'inventaire</Button> : null}
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!inv ? <Spinner /> : (
        <>
          <p className="muted" style={{ marginBottom: 12 }}>
            {inv.entete.libelle} · créé par {inv.entete.creePar} le {formatDateTime(inv.entete.createdAt)}
            {inv.entete.validePar ? ` · validé par ${inv.entete.validePar} le ${formatDateTime(inv.entete.valideLe)}` : ''}
          </p>
          {open ? <Alert type="info">Laissez vide une ligne non comptée : elle ne sera pas ajustée. À la validation, le stock de chaque ligne comptée devient égal au comptage.</Alert> : null}
          <div className="table-wrap" style={{ maxHeight: '55vh' }}>
            <table className="table table-edit">
              <thead><tr><th>Article</th><th>Lot</th><th className="num">Théorique</th><th>Compté</th><th className="num">Écart</th></tr></thead>
              <tbody>{inv.lignes.map((l) => {
                const c = counts[l.id];
                const ecart = c === '' || c === undefined ? null : Number(normalizeDecimal(c)) - Number(l.theorique);
                return (
                  <tr key={l.id}>
                    <td><strong>{l.article}</strong><div className="muted" style={{ fontSize: 12 }}>{l.reference}</div></td>
                    <td>{l.lot || '—'}</td>
                    <td className="num">{formatQty(l.theorique, l.unite)}</td>
                    <td>{open ? <input className="input" inputMode="decimal" style={{ width: 120 }} value={c ?? ''}
                                       onChange={(e) => setCounts({ ...counts, [l.id]: e.target.value })} />
                      : formatQty(l.comptee, l.unite)}</td>
                    <td className="num" style={{ color: ecart > 0 ? 'var(--success)' : ecart < 0 ? 'var(--danger)' : undefined }}>
                      {ecart == null || Number.isNaN(ecart) ? '—' : `${ecart > 0 ? '+' : ''}${formatQty(ecart, l.unite)}`}
                    </td>
                  </tr>
                );
              })}</tbody>
            </table>
          </div>
        </>
      )}
      {confirm ? (
        <ConfirmDialog title={confirm === 'validate' ? 'Valider cet inventaire ?' : 'Annuler cet inventaire ?'}
                       message={confirm === 'validate' ? 'Des mouvements d\'inventaire seront créés pour chaque écart. Cette opération est définitive.' : 'Aucun mouvement ne sera créé.'}
                       confirmLabel={confirm === 'validate' ? 'Valider' : 'Annuler l\'inventaire'} danger={confirm === 'cancel'}
                       onConfirm={() => finish(confirm)} onCancel={() => setConfirm(null)} />
      ) : null}
    </Modal>
  );
}

export default function InventoriesTab({ canAdjust, reloadKey }) {
  const list = usePagedList(inventoryService.list, { reloadKey });
  const [creating, setCreating] = useState(false);
  const [libelle, setLibelle] = useState('');
  const [openId, setOpenId] = useState(null);
  const [error, setError] = useState('');

  const create = async () => {
    setError('');
    try {
      const inv = await inventoryService.create({ libelle: libelle.trim(), articleIds: [] });
      setCreating(false);
      setLibelle('');
      list.reload();
      setOpenId(inv.entete.id);
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="panel">
      <div className="section-title">
        <h2>Inventaires physiques</h2>
        {canAdjust ? <Button size="sm" onClick={() => setCreating(true)}><Plus size={16} />Nouvel inventaire</Button> : null}
      </div>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} rowKey="id" onRowClick={(i) => setOpenId(i.id)}
                   empty={<EmptyState icon={ClipboardList} title="Aucun inventaire" text="Un inventaire compare le stock théorique au comptage réel." />}
                   columns={[
                     { key: 'n', header: 'Numéro', render: (i) => <strong>{i.numero}</strong> },
                     { key: 'l', header: 'Libellé', render: (i) => i.libelle },
                     { key: 'd', header: 'Créé le', render: (i) => formatDateTime(i.createdAt) },
                     { key: 'c', header: 'Lignes comptées', render: (i) => `${i.nbComptees} / ${i.nbLignes}` },
                     { key: 'e', header: 'Écarts', render: (i) => (i.nbEcarts ? <span className="badge badge-warn">{i.nbEcarts}</span> : '—') },
                     { key: 's', header: 'Statut', render: (i) => <span className={`badge ${i.statut === 'VALIDE' ? 'badge-ok' : i.statut === 'ANNULE' ? 'badge-off' : 'badge-role'}`}>{INVENTORY_STATUS[i.statut]}</span> },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {creating ? (
        <Modal title="Nouvel inventaire" onClose={() => setCreating(false)}
               footer={(<><Button variant="secondary" onClick={() => setCreating(false)}>Annuler</Button>
                 <Button disabled={!libelle.trim()} onClick={create}>Créer et commencer le comptage</Button></>)}>
          <Alert type="error">{error}</Alert>
          <p style={{ marginBottom: 12 }}>Le stock théorique de tous les articles actifs est photographié maintenant.</p>
          <div className="field"><label>Libellé *</label>
            <input className="input" value={libelle} onChange={(e) => setLibelle(e.target.value)} placeholder="Ex. Inventaire fin de campagne" autoFocus /></div>
        </Modal>
      ) : null}
      {openId ? <InventoryModal id={openId} canAdjust={canAdjust} onClose={() => setOpenId(null)} onChanged={list.reload} /> : null}
    </div>
  );
}
