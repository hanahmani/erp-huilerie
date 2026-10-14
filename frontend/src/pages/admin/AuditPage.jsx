import { History } from 'lucide-react';
import { usePagedList } from '../../hooks/usePagedList';
import { auditService } from '../../services/auditService';
import PageHeader from '../../components/common/PageHeader';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import { dayRange, formatDateTime } from '../../utils/format';

const ACTIONS = [
  ['LOGIN', 'Connexions'], ['USER', 'Utilisateurs'], ['ROLE', 'Rôles'], ['PASSWORD', 'Mots de passe'], ['SALE', 'Ventes'],
  ['TICKET', 'Impressions'], ['PURCHASE', 'Achats'], ['SUPPLIER_PAYMENT', 'Paiements fournisseurs'], ['STOCK', 'Stock'],
  ['LOT', 'Lots'], ['INVENTORY', 'Inventaires'], ['EXPENSE', 'Dépenses'], ['CASH', 'Caisse'], ['SETTINGS', 'Paramètres'],
  ['CREATE', 'Créations'], ['UPDATE', 'Modifications'], ['DEACTIVATE', 'Désactivations'],
];

export default function AuditPage() {
  const list = usePagedList((p) => auditService.search({ ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined }), {}, 50);
  return (
    <>
      <PageHeader title="Journal d'activité" description="Toutes les opérations sensibles, avec leur auteur et leur date. Ce journal ne peut pas être modifié." />
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.username} onChange={(v) => list.updateFilter('username', v)} placeholder="Utilisateur" />
          <FilterSelect label="Action" allLabel="Toutes les actions" value={list.filters.action}
                        options={ACTIONS.map(([value, label]) => ({ value, label }))} onChange={(v) => list.updateFilter('action', v)} />
          <DateInput label="Du" value={list.filters.fromDate} onChange={(v) => list.updateFilter('fromDate', v)} />
          <DateInput label="Au" value={list.filters.toDate} onChange={(v) => list.updateFilter('toDate', v)} />
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} empty={<EmptyState icon={History} title="Aucune entrée" text="Aucune opération ne correspond aux filtres." />}
                     columns={[
                       { key: 'd', header: 'Date', render: (a) => formatDateTime(a.date) },
                       { key: 'u', header: 'Utilisateur', render: (a) => <strong>{a.username || '—'}</strong> },
                       { key: 'a', header: 'Action', render: (a) => <span className="badge badge-role">{a.action}</span> },
                       { key: 'e', header: 'Élément', render: (a) => (a.entityType ? `${a.entityType}${a.entityId ? ` #${a.entityId}` : ''}` : '—') },
                       { key: 'x', header: 'Détails', render: (a) => <span className="muted">{a.details || ''}</span> },
                       { key: 'ip', header: 'Adresse IP', render: (a) => <span className="muted">{a.ip || '—'}</span> },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
    </>
  );
}
