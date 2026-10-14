import { useState } from 'react';
import { Download, Plus, ShoppingBasket } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { purchaseService } from '../../services/purchaseService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import PurchaseFormModal from './PurchaseFormModal';
import PurchaseDetailModal from './PurchaseDetailModal';
import { formatDT } from '../../utils/format';
import { PURCHASE_STATUS, PURCHASE_STATUS_CLASS, toOptions } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

const columns = [
  { key: 'n', header: 'Numéro', render: (p) => <strong>{p.numero}</strong>, csv: (p) => p.numero },
  { key: 'd', header: 'Date', render: (p) => new Date(`${p.dateCommande}T12:00:00`).toLocaleDateString('fr-FR'), csv: (p) => p.dateCommande },
  { key: 'f', header: 'Fournisseur', render: (p) => p.fournisseurNom, csv: (p) => p.fournisseurNom },
  { key: 'r', header: 'Réf. facture', render: (p) => p.refFacture || '—', csv: (p) => p.refFacture },
  { key: 't', header: 'Total TTC', align: 'right', render: (p) => formatDT(p.totalTtc), csv: (p) => Number(p.totalTtc) },
  { key: 'p', header: 'Payé', align: 'right', render: (p) => formatDT(p.montantPaye), csv: (p) => Number(p.montantPaye) },
  { key: 'x', header: 'Reste', align: 'right', render: (p) => (Number(p.reste) ? <strong>{formatDT(p.reste)}</strong> : '—'), csv: (p) => Number(p.reste) },
  { key: 's', header: 'Statut', render: (p) => <span className={`badge ${PURCHASE_STATUS_CLASS[p.statut]}`}>{PURCHASE_STATUS[p.statut]}</span>, csv: (p) => PURCHASE_STATUS[p.statut] },
];

export default function PurchasesPage() {
  const { can } = useAuth();
  const list = usePagedList(purchaseService.list, { unpaid: false });
  const [creating, setCreating] = useState(false);
  const [openId, setOpenId] = useState(null);

  const exportAll = async () => {
    const page = await purchaseService.list({ ...list.filters, page: 0, size: 100 });
    exportCsv('achats', columns.map((c) => ({ header: c.header, value: c.csv })), page.content);
  };

  return (
    <>
      <PageHeader title="Achats" description="Commandes aux fournisseurs, réceptions et paiements. Le stock n'augmente qu'à la réception."
                  actions={(
                    <>
                      <Button variant="secondary" onClick={exportAll}><Download size={18} />Exporter</Button>
                      {can('PURCHASE_CREATE') ? <Button onClick={() => setCreating(true)}><Plus size={18} />Nouvelle commande</Button> : null}
                    </>
                  )} />
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="N°, fournisseur, réf. facture" />
          <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut} options={toOptions(PURCHASE_STATUS)}
                        onChange={(v) => list.updateFilter('statut', v)} />
          <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
          <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
          <label className="check" style={{ height: 40 }}>
            <input type="checkbox" checked={list.filters.unpaid === true} onChange={(e) => list.updateFilter('unpaid', e.target.checked)} />
            Non soldées
          </label>
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} columns={columns} onRowClick={(p) => setOpenId(p.id)}
                     empty={<EmptyState icon={ShoppingBasket} title="Aucune commande" text="Créez une commande pour un fournisseur." />} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {creating ? <PurchaseFormModal onClose={() => setCreating(false)} onSaved={(p) => { setCreating(false); list.reload(); setOpenId(p.id); }} /> : null}
      {openId ? <PurchaseDetailModal id={openId} onClose={() => setOpenId(null)} onChanged={list.reload} /> : null}
    </>
  );
}
