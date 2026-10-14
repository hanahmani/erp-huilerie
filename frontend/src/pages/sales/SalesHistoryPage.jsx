import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, ReceiptText } from 'lucide-react';
import { usePagedList } from '../../hooks/usePagedList';
import { useAuth } from '../../context/AuthContext';
import { salesService } from '../../services/salesService';
import { clientService } from '../../services/clientService';
import PageHeader from '../../components/common/PageHeader';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import SaleDetailModal from './SaleDetailModal';
import { dayRange, formatDateTime, formatDT } from '../../utils/format';
import { PAYMENT_STATUS } from '../../utils/labels';

const STATUS_CLASS = { PAYEE: 'badge-ok', PARTIELLE: 'badge-warn', NON_PAYEE: 'badge-warn', ANNULEE: 'badge-off' };

export default function SalesHistoryPage() {
  const { can } = useAuth();
  const [cashiers, setCashiers] = useState([]);
  const [clients, setClients] = useState([]);
  const [openId, setOpenId] = useState(null);
  const list = usePagedList((p) => salesService.list({
    ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined,
  }));

  useEffect(() => {
    salesService.cashiers().then(setCashiers).catch(() => {});
    if (can('CLIENT_VIEW')) clientService.list({ size: 100 }).then((p) => setClients(p.content)).catch(() => {});
  }, [can]);

  return (
    <>
      <PageHeader title="Historique des ventes" description="Toutes les ventes validées. Cliquez sur une vente pour la consulter ou réimprimer son ticket."
                  actions={can('SALES_CREATE') ? <Link className="btn btn-primary" to="/ventes/rapide"><Plus size={18} />Nouvelle vente</Link> : null} />
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.numero} onChange={(v) => list.updateFilter('numero', v)} placeholder="N° de ticket" />
          <DateInput label="Du" value={list.filters.fromDate} onChange={(v) => list.updateFilter('fromDate', v)} />
          <DateInput label="Au" value={list.filters.toDate} onChange={(v) => list.updateFilter('toDate', v)} />
          <FilterSelect label="Client" allLabel="Tous les clients" value={list.filters.clientId}
                        options={clients.map((c) => ({ value: c.id, label: c.nom }))} onChange={(v) => list.updateFilter('clientId', v)} />
          <FilterSelect label="Caissier" allLabel="Tous les caissiers" value={list.filters.cashierId}
                        options={cashiers.map((c) => ({ value: c.id, label: c.nom }))} onChange={(v) => list.updateFilter('cashierId', v)} />
          <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut}
                        options={[{ value: 'VALIDEE', label: 'Validées' }, { value: 'ANNULEE', label: 'Annulées' }]}
                        onChange={(v) => list.updateFilter('statut', v)} />
        </Toolbar>
        <Alert type="error">{list.error}</Alert>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} onRowClick={(s) => setOpenId(s.id)}
                     empty={<EmptyState icon={ReceiptText} title="Aucune vente" text="Les ventes validées en caisse apparaîtront ici." />}
                     columns={[
                       { key: 'n', header: 'Ticket', render: (s) => <strong>{s.numeroTicket}</strong> },
                       { key: 'd', header: 'Date', render: (s) => formatDateTime(s.date) },
                       { key: 'c', header: 'Client', render: (s) => s.clientNom || <span className="muted">Comptoir</span> },
                       { key: 'k', header: 'Caissier', render: (s) => s.caissierNom },
                       { key: 't', header: 'Total', align: 'right', render: (s) => <strong>{formatDT(s.total)}</strong> },
                       { key: 'p', header: 'Payé', align: 'right', render: (s) => formatDT(s.montantPaye) },
                       { key: 'r', header: 'Reste', align: 'right', render: (s) => (Number(s.resteAPayer) ? formatDT(s.resteAPayer) : '—') },
                       { key: 's', header: 'Paiement', render: (s) => <span className={`badge ${STATUS_CLASS[s.paiementStatut]}`}>{PAYMENT_STATUS[s.paiementStatut]}</span> },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {openId ? <SaleDetailModal saleId={openId} onClose={() => setOpenId(null)} onChanged={list.reload} /> : null}
    </>
  );
}
