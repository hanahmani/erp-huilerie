import { useState } from 'react';
import { Download, Droplets, Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { pressingService } from '../../services/operationsService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import ReceptionModal from './ReceptionModal';
import PressingDetailModal from './PressingDetailModal';
import { dayRange, formatDateTime, formatDT, formatQty } from '../../utils/format';
import { PRESSING_STATUS, PRESSING_STATUS_CLASS, toOptions } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

export default function PressingPage() {
  const { can } = useAuth();
  const fetcher = (p) => pressingService.list({ ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined });
  const list = usePagedList(fetcher, {});
  const [receiving, setReceiving] = useState(false);
  const [open, setOpen] = useState(null);   // { id, initial }

  const exportAll = async () => {
    const page = await fetcher({ ...list.filters, page: 0, size: 100 });
    exportCsv('pressages', [
      { header: 'N°', value: (p) => p.numero }, { header: 'Date', value: (p) => formatDateTime(p.dateReception) },
      { header: 'Client', value: (p) => p.clientNom }, { header: 'Poids net (kg)', value: (p) => Number(p.poidsNet) },
      { header: 'Huile obtenue', value: (p) => (p.huileObtenue == null ? '' : Number(p.huileObtenue)) },
      { header: 'Rendement', value: (p) => (p.rendement == null ? '' : Number(p.rendement)) },
      { header: 'À remettre', value: (p) => Number(p.huileARemettre) }, { header: 'Montant', value: (p) => Number(p.montant) },
      { header: 'Reste à payer', value: (p) => Number(p.reste) }, { header: 'Statut', value: (p) => PRESSING_STATUS[p.statut] },
    ], page.content);
  };

  return (
    <>
      <PageHeader title="Pressage client" description="Olives apportées par les clients. Leur huile reste leur propriété : elle est stockée à part et ne peut jamais être vendue."
                  actions={(
                    <>
                      <Button variant="secondary" onClick={exportAll}><Download size={18} />Exporter</Button>
                      {can('PRESSING_MANAGE') ? <Button onClick={() => setReceiving(true)}><Plus size={18} />Réception d'olives</Button> : null}
                    </>
                  )} />
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="N°, client, téléphone" />
          <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut} options={toOptions(PRESSING_STATUS)}
                        onChange={(v) => list.updateFilter('statut', v)} />
          <DateInput label="Du" value={list.filters.fromDate} onChange={(v) => list.updateFilter('fromDate', v)} />
          <DateInput label="Au" value={list.filters.toDate} onChange={(v) => list.updateFilter('toDate', v)} />
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} onRowClick={(p) => setOpen({ id: p.id })}
                     empty={<EmptyState icon={Droplets} title="Aucun pressage" text="Enregistrez la réception et la pesée des olives d'un client." />}
                     columns={[
                       { key: 'n', header: 'N°', render: (p) => <strong>{p.numero}</strong> },
                       { key: 'd', header: 'Reçu le', render: (p) => formatDateTime(p.dateReception) },
                       { key: 'c', header: 'Client', render: (p) => p.clientNom },
                       { key: 'k', header: 'Poids net', align: 'right', render: (p) => formatQty(p.poidsNet, 'kg') },
                       { key: 'h', header: 'Huile', align: 'right', render: (p) => (p.huileObtenue != null ? formatQty(p.huileObtenue) : '—') },
                       { key: 'y', header: 'Rendement', align: 'right', render: (p) => (p.rendement != null ? `${Number(p.rendement).toFixed(1)} L/100 kg` : '—') },
                       { key: 'r', header: 'À remettre', align: 'right', render: (p) => (Number(p.huileARemettre) ? <strong>{formatQty(p.huileARemettre)}</strong> : '—') },
                       { key: 'm', header: 'Reste à payer', align: 'right', render: (p) => (Number(p.reste) ? formatDT(p.reste) : '—') },
                       { key: 's', header: 'Statut', render: (p) => <span className={`badge ${PRESSING_STATUS_CLASS[p.statut]}`}>{PRESSING_STATUS[p.statut]}</span> },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {receiving ? <ReceptionModal onClose={() => setReceiving(false)} onDone={(p) => { setReceiving(false); list.reload(); setOpen({ id: p.id, initial: p }); }} /> : null}
      {open ? <PressingDetailModal id={open.id} initial={open.initial} onClose={() => setOpen(null)} onChanged={list.reload} /> : null}
    </>
  );
}
