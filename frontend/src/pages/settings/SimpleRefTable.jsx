import { useCallback, useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import EntityFormModal from '../../components/forms/EntityFormModal';

/** Petit référentiel sans pagination (unités, catégories). */
export default function SimpleRefTable({ load, save, setStatus, columns, fields, label, createLabel, emptyText, canManage }) {
  const [rows, setRows] = useState(null);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const refresh = useCallback(() => load().then(setRows).catch((e) => setError(e.message)), [load]);
  useEffect(() => { refresh(); }, [refresh]);

  const toggle = async (r) => {
    try {
      await setStatus(r.id, !r.actif);
      refresh();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="panel">
      <div className="section-title">
        <h2>{label}</h2>
        {canManage ? <Button size="sm" onClick={() => setEditing({})}><Plus size={16} />{createLabel}</Button> : null}
      </div>
      {error ? <div className="panel-body"><Alert type="error">{error}</Alert></div> : null}
      {!rows ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={rows}
                   columns={[
                     ...columns,
                     { key: 'actif', header: 'Statut', render: (r) => <StatusBadge active={r.actif} /> },
                     ...(canManage ? [{
                       key: 'a', header: '', align: 'right',
                       render: (r) => (
                         <div className="actions">
                           <Button size="sm" variant="secondary" onClick={() => setEditing(r)}>Modifier</Button>
                           <Button size="sm" variant={r.actif ? 'danger' : 'secondary'} onClick={() => toggle(r)}>
                             {r.actif ? 'Désactiver' : 'Réactiver'}
                           </Button>
                         </div>
                       ),
                     }] : []),
                   ]}
                   empty={<EmptyState title={`Aucun élément`} text={emptyText} />} />
      )}
      {editing ? (
        <EntityFormModal title={editing.id ? 'Modifier' : createLabel} fields={fields} initial={editing}
                         onClose={() => setEditing(null)}
                         onSubmit={async (p) => { await save(editing.id, p); setEditing(null); refresh(); }} />
      ) : null}
    </div>
  );
}
