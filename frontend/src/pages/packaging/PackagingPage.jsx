import { useState } from 'react';
import { Download, Package, Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { packagingService } from '../../services/operationsService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import PackagingModal from './PackagingModal';
import { dayRange, formatDateTime, formatQty } from '../../utils/format';
import { exportCsv } from '../../utils/exportCsv';

export default function PackagingPage() {
  const { can } = useAuth();
  const manage = can('PACKAGING_MANAGE');
  const fetcher = (p) => packagingService.list({ ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined });
  const list = usePagedList(fetcher, {});
  const [creating, setCreating] = useState(false);
  const [cancelling, setCancelling] = useState(null);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const cancel = async () => {
    setError('');
    try {
      await packagingService.cancel(cancelling.id, motif.trim());
      setCancelling(null);
      setMotif('');
      setMessage('Conditionnement annulé : le vrac et les emballages ont été remis en stock.');
      list.reload();
    } catch (e) {
      setError(e.message);
      setCancelling(null);
    }
  };

  const exportAll = async () => {
    const page = await fetcher({ ...list.filters, page: 0, size: 100 });
    exportCsv('conditionnements', [
      { header: 'N°', value: (o) => o.numero }, { header: 'Date', value: (o) => formatDateTime(o.createdAt) },
      { header: 'Vrac', value: (o) => o.source }, { header: 'Lot', value: (o) => o.sourceLot },
      { header: 'Produit', value: (o) => o.produit }, { header: 'Quantité', value: (o) => Number(o.quantiteProduite) },
      { header: 'Volume (L)', value: (o) => Number(o.volumeUtilise) }, { header: 'Statut', value: (o) => o.statut },
    ], page.content);
  };

  return (
    <>
      <PageHeader title="Conditionnement" description="Mise en bouteilles et en bidons de l'huile en vrac de l'huilerie, avec consommation des emballages."
                  actions={(
                    <>
                      <Button variant="secondary" onClick={exportAll}><Download size={18} />Exporter</Button>
                      {manage ? <Button onClick={() => setCreating(true)}><Plus size={18} />Nouveau conditionnement</Button> : null}
                    </>
                  )} />
      <Alert type="error">{error || list.error}</Alert>
      <Alert type="success">{message}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="N° ou produit" />
          <FilterSelect label="Statut" allLabel="Tous" value={list.filters.statut}
                        options={[{ value: 'VALIDE', label: 'Validés' }, { value: 'ANNULE', label: 'Annulés' }]} onChange={(v) => list.updateFilter('statut', v)} />
          <DateInput label="Du" value={list.filters.fromDate} onChange={(v) => list.updateFilter('fromDate', v)} />
          <DateInput label="Au" value={list.filters.toDate} onChange={(v) => list.updateFilter('toDate', v)} />
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content}
                     empty={<EmptyState icon={Package} title="Aucun conditionnement" text="Transformez de l'huile en vrac en bouteilles ou bidons prêts à vendre." />}
                     columns={[
                       { key: 'n', header: 'N°', render: (o) => <strong>{o.numero}</strong> },
                       { key: 'd', header: 'Date', render: (o) => formatDateTime(o.createdAt) },
                       { key: 's', header: 'Vrac utilisé', render: (o) => <>{o.source}{o.sourceLot ? <div className="muted" style={{ fontSize: 12 }}>lot {o.sourceLot}</div> : null}</> },
                       { key: 'v', header: 'Volume', align: 'right', render: (o) => formatQty(o.volumeUtilise, o.sourceUnite) },
                       { key: 'p', header: 'Produit fini', render: (o) => o.produit },
                       { key: 'q', header: 'Quantité', align: 'right', render: (o) => <strong>{formatQty(o.quantiteProduite, o.produitUnite)}</strong> },
                       { key: 'c', header: 'Emballages', render: (o) => (o.consommations.length ? o.consommations.map((c) => `${c.article} × ${formatQty(c.quantite)}`).join(', ') : '—') },
                       { key: 'st', header: 'Statut', render: (o) => (o.statut === 'VALIDE' ? <span className="badge badge-ok">Validé</span> : <span className="badge badge-off" title={o.motifAnnulation}>Annulé</span>) },
                       ...(manage ? [{ key: 'x', header: '', align: 'right', render: (o) => (o.statut === 'VALIDE' ? <Button size="sm" variant="danger" onClick={() => setCancelling(o)}>Annuler</Button> : null) }] : []),
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {creating ? <PackagingModal onClose={() => setCreating(false)} onDone={(o) => { setCreating(false); setMessage(`Conditionnement ${o.numero} validé.`); list.reload(); }} /> : null}
      {cancelling ? (
        <Modal title={`Annuler ${cancelling.numero}`} onClose={() => setCancelling(null)}
               footer={(<><Button variant="secondary" onClick={() => setCancelling(null)}>Ne pas annuler</Button>
                 <Button variant="danger" disabled={!motif.trim()} onClick={cancel}>Annuler le conditionnement</Button></>)}>
          <p style={{ marginBottom: 12 }}>Les produits finis sortent du stock (impossible s'ils ont déjà été vendus), le vrac et les emballages y reviennent.</p>
          <div className="field"><label>Motif *</label><input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} autoFocus /></div>
        </Modal>
      ) : null}
    </>
  );
}
