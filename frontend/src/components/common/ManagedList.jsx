import { useState } from 'react';
import { Plus } from 'lucide-react';
import { usePagedList } from '../../hooks/usePagedList';
import { useAuth } from '../../context/AuthContext';
import PageHeader from './PageHeader';
import Button from './Button';
import Alert from './Alert';
import Spinner from './Spinner';
import EmptyState from './EmptyState';
import ConfirmDialog from './ConfirmDialog';
import StatusBadge from './StatusBadge';
import DataTable from '../tables/DataTable';
import Pagination from '../tables/Pagination';
import { Toolbar, SearchBox, FilterSelect } from '../tables/Toolbar';
import EntityFormModal from '../forms/EntityFormModal';

/**
 * Liste paginée + création / modification / désactivation, branchée sur l'API.
 * Utilisée par clients, fournisseurs et articles.
 */
export default function ManagedList({
  title, description, icon, entityLabel, createLabel, managePermission,
  fetcher, columns, filters = [], fields, toForm, onSave, onStatus, emptyText, extraActions,
}) {
  const { can } = useAuth();
  const canManage = can(managePermission);
  const list = usePagedList(fetcher, { actif: 'true' });
  const [editing, setEditing] = useState(null);      // {} = création, objet = modification
  const [toggling, setToggling] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const fullColumns = [
    ...columns,
    { key: 'actif', header: 'Statut', render: (r) => <StatusBadge active={r.actif} /> },
    ...(canManage ? [{
      key: 'actions', header: '', align: 'right',
      render: (r) => (
        <div className="actions" onClick={(e) => e.stopPropagation()}>
          {extraActions?.(r)}
          <Button size="sm" variant="secondary" onClick={() => setEditing(r)}>Modifier</Button>
          <Button size="sm" variant={r.actif ? 'danger' : 'secondary'} onClick={() => setToggling(r)}>
            {r.actif ? 'Désactiver' : 'Réactiver'}
          </Button>
        </div>
      ),
    }] : []),
  ];

  const save = async (payload) => {
    const isNew = !editing.id;
    await onSave(editing.id, payload);
    setEditing(null);
    setMessage(isNew ? `${entityLabel} créé.` : `${entityLabel} modifié.`);
    list.reload();
  };

  const toggle = async () => {
    setBusy(true);
    try {
      await onStatus(toggling.id, !toggling.actif);
      setMessage(toggling.actif ? `${entityLabel} désactivé.` : `${entityLabel} réactivé.`);
      list.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setToggling(null);
    }
  };

  return (
    <>
      <PageHeader title={title} description={description}
                  actions={canManage ? <Button onClick={() => setEditing({})}><Plus size={18} />{createLabel}</Button> : null} />
      <Alert type="success">{message}</Alert>
      <Alert type="error">{error || list.error}</Alert>

      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} />
          {filters.map((f) => (
            <FilterSelect key={f.key} label={f.label} value={list.filters[f.key]} options={f.options}
                          allLabel={f.allLabel} onChange={(v) => list.updateFilter(f.key, v)} />
          ))}
          <FilterSelect label="Statut" value={list.filters.actif} allLabel="Actifs et inactifs"
                        options={[{ value: 'true', label: 'Actifs' }, { value: 'false', label: 'Inactifs' }]}
                        onChange={(v) => list.updateFilter('actif', v)} />
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable columns={fullColumns} rows={list.data.content}
                     onRowClick={canManage ? (r) => setEditing(r) : undefined}
                     empty={<EmptyState icon={icon} title={`Aucun résultat`}
                                        text={list.filters.q ? 'Aucun élément ne correspond à la recherche.' : emptyText}
                                        action={canManage && !list.filters.q
                                          ? <Button onClick={() => setEditing({})}><Plus size={18} />{createLabel}</Button> : null} />} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements}
                    onChange={list.setPage} />
      </div>

      {editing ? (
        <EntityFormModal title={editing.id ? `Modifier : ${editing.nom ?? ''}` : createLabel}
                         fields={typeof fields === 'function' ? fields(editing) : fields}
                         initial={toForm(editing)} onSubmit={save} onClose={() => setEditing(null)} />
      ) : null}

      {toggling ? (
        <ConfirmDialog title={toggling.actif ? `Désactiver ${toggling.nom} ?` : `Réactiver ${toggling.nom} ?`}
                       message={toggling.actif
                         ? "Il n'apparaîtra plus dans les listes de saisie. Son historique est conservé."
                         : 'Il sera de nouveau proposé dans les saisies.'}
                       confirmLabel={toggling.actif ? 'Désactiver' : 'Réactiver'} danger={toggling.actif}
                       loading={busy} onConfirm={toggle} onCancel={() => setToggling(null)} />
      ) : null}
    </>
  );
}
