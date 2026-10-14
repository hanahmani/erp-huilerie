import { useState } from 'react';
import { Download, FileText, Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { salesDocService } from '../../services/salesDocService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import SalesDocFormModal from './SalesDocFormModal';
import SalesDocDetailModal from './SalesDocDetailModal';
import { formatDT } from '../../utils/format';
import { DOC_STATUS, DOC_STATUS_CLASS, toOptions } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

const TEXTS = {
  DEVIS: ['Devis', 'Propositions de prix aux clients. Un devis validé se transforme en commande ou en facture.', 'Nouveau devis'],
  COMMANDE: ['Commandes clients', 'Commandes confirmées, à préparer puis livrer ou facturer.', 'Nouvelle commande'],
  LIVRAISON: ['Bons de livraison', 'La marchandise sort du stock à la validation du bon de livraison.', 'Nouveau bon de livraison'],
  FACTURE: ['Factures clients', 'Numéro définitif à la validation, TVA et timbre fiscal, paiements partiels ; annulation par avoir.', 'Nouvelle facture'],
  AVOIR: ['Avoirs', "Annulations de factures, émises depuis la facture concernée.", null],
};
const date = (iso) => (iso ? new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR') : '—');

export default function SalesDocsPage({ type }) {
  const { can } = useAuth();
  const list = usePagedList((p) => salesDocService.list({ ...p, type }), { unpaid: false, type });
  const [creating, setCreating] = useState(false);
  const [openId, setOpenId] = useState(null);
  const [title, description, createLabel] = TEXTS[type];
  const invoice = type === 'FACTURE';

  const columns = [
    { key: 'n', header: 'Numéro', render: (d) => <strong>{d.numero || <span className="muted">Brouillon</span>}</strong>, csv: (d) => d.numero || 'Brouillon' },
    { key: 'd', header: 'Date', render: (d) => date(d.dateDoc), csv: (d) => d.dateDoc },
    { key: 'c', header: 'Client', render: (d) => d.clientNom, csv: (d) => d.clientNom },
    ...(invoice ? [{ key: 'e', header: 'Échéance', render: (d) => {
      const late = d.statut === 'VALIDE' && Number(d.reste) > 0 && d.echeance && new Date(`${d.echeance}T23:59:59`) < new Date();
      return <span style={late ? { color: 'var(--danger)', fontWeight: 600 } : undefined}>{date(d.echeance)}</span>;
    }, csv: (d) => d.echeance }] : []),
    { key: 't', header: 'Total TTC', align: 'right', render: (d) => formatDT(d.totalTtc), csv: (d) => Number(d.totalTtc) },
    ...(invoice ? [{ key: 'r', header: 'Reste', align: 'right', render: (d) => (Number(d.reste) && d.statut === 'VALIDE' ? <strong>{formatDT(d.reste)}</strong> : '—'), csv: (d) => Number(d.reste) }] : []),
    { key: 's', header: 'Statut', render: (d) => <span className={`badge ${DOC_STATUS_CLASS[d.statut]}`}>{DOC_STATUS[d.statut]}</span>, csv: (d) => DOC_STATUS[d.statut] },
  ];

  const exportAll = async () => {
    const page = await salesDocService.list({ ...list.filters, type, page: 0, size: 100 });
    exportCsv(title.toLowerCase().replace(/\s+/g, '-'), columns.map((c) => ({ header: c.header, value: c.csv })), page.content);
  };

  return (
    <>
      <PageHeader title={title} description={description}
                  actions={(
                    <>
                      <Button variant="secondary" onClick={exportAll}><Download size={18} />Exporter</Button>
                      {createLabel && can('SALESDOC_MANAGE') ? <Button onClick={() => setCreating(true)}><Plus size={18} />{createLabel}</Button> : null}
                    </>
                  )} />
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="Numéro, client, réf." />
          <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut} options={toOptions(DOC_STATUS)} onChange={(v) => list.updateFilter('statut', v)} />
          <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
          <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
          {invoice ? <label className="check" style={{ height: 40 }}><input type="checkbox" checked={list.filters.unpaid === true} onChange={(e) => list.updateFilter('unpaid', e.target.checked)} />Impayées</label> : null}
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} columns={columns} onRowClick={(d) => setOpenId(d.id)}
                     empty={<EmptyState icon={FileText} title="Aucun document" text={description} />} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {creating ? <SalesDocFormModal type={type} onClose={() => setCreating(false)} onSaved={(d) => { setCreating(false); list.reload(); setOpenId(d.id); }} /> : null}
      {openId ? <SalesDocDetailModal key={openId} id={openId} onClose={() => setOpenId(null)} onChanged={list.reload} onOpen={setOpenId} /> : null}
    </>
  );
}
