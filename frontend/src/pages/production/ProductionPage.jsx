import { useState } from 'react';
import { Download, Factory, Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { productionService } from '../../services/operationsService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import ProductionFormModal from './ProductionFormModal';
import ProductionDetailModal from './ProductionDetailModal';
import { formatQty } from '../../utils/format';
import { OIL_CATEGORIES, PRODUCTION_STATUS, PRODUCTION_STATUS_CLASS, toOptions } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

export default function ProductionPage() {
  const { can } = useAuth();
  const list = usePagedList(productionService.list, {});
  const [creating, setCreating] = useState(false);
  const [openId, setOpenId] = useState(null);

  const exportAll = async () => {
    const page = await productionService.list({ ...list.filters, page: 0, size: 100 });
    exportCsv('production', [
      { header: 'N°', value: (o) => o.numero }, { header: 'Date', value: (o) => o.datePrevue }, { header: 'Ligne', value: (o) => o.ligne },
      { header: 'Huile', value: (o) => o.huileArticle }, { header: 'Olives (kg)', value: (o) => Number(o.olivesKg) },
      { header: 'Huile obtenue', value: (o) => (o.huileLitres == null ? '' : Number(o.huileLitres)) },
      { header: 'Rendement', value: (o) => (o.rendement == null ? '' : Number(o.rendement)) },
      { header: 'Catégorie', value: (o) => (o.categorie ? OIL_CATEGORIES[o.categorie] : '') }, { header: 'Statut', value: (o) => PRODUCTION_STATUS[o.statut] },
    ], page.content);
  };

  return (
    <>
      <PageHeader title="Production" description="Trituration des olives de l'huilerie : les olives sortent du stock au démarrage, l'huile et les sous-produits y entrent à la fin."
                  actions={(
                    <>
                      <Button variant="secondary" onClick={exportAll}><Download size={18} />Exporter</Button>
                      {can('PRODUCTION_MANAGE') ? <Button onClick={() => setCreating(true)}><Plus size={18} />Nouvelle trituration</Button> : null}
                    </>
                  )} />
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="N° ou ligne" />
          <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut} options={toOptions(PRODUCTION_STATUS)}
                        onChange={(v) => list.updateFilter('statut', v)} />
          <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
          <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} onRowClick={(o) => setOpenId(o.id)}
                     empty={<EmptyState icon={Factory} title="Aucune trituration" text="Planifiez la trituration de lots d'olives achetés par l'huilerie." />}
                     columns={[
                       { key: 'n', header: 'N°', render: (o) => <strong>{o.numero}</strong> },
                       { key: 'd', header: 'Date', render: (o) => new Date(`${o.datePrevue}T12:00:00`).toLocaleDateString('fr-FR') },
                       { key: 'l', header: 'Ligne', render: (o) => o.ligne || '—' },
                       { key: 'o', header: 'Olives', align: 'right', render: (o) => formatQty(o.olivesKg, 'kg') },
                       { key: 'h', header: 'Huile', align: 'right', render: (o) => (o.huileLitres != null ? formatQty(o.huileLitres) : '—') },
                       { key: 'r', header: 'Rendement', align: 'right', render: (o) => (o.rendement != null ? `${Number(o.rendement).toFixed(2)} L/100 kg` : '—') },
                       { key: 'c', header: 'Catégorie', render: (o) => (o.categorie ? OIL_CATEGORIES[o.categorie] : '—') },
                       { key: 's', header: 'Statut', render: (o) => <span className={`badge ${PRODUCTION_STATUS_CLASS[o.statut]}`}>{PRODUCTION_STATUS[o.statut]}</span> },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {creating ? <ProductionFormModal onClose={() => setCreating(false)} onSaved={(o) => { setCreating(false); list.reload(); setOpenId(o.id); }} /> : null}
      {openId ? <ProductionDetailModal id={openId} onClose={() => setOpenId(null)} onChanged={list.reload} /> : null}
    </>
  );
}
