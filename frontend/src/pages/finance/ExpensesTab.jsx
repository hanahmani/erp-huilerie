import { useEffect, useState } from 'react';
import { Plus, Receipt } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { financeService } from '../../services/financeService';
import { referenceService } from '../../services/referenceService';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import EntityFormModal from '../../components/forms/EntityFormModal';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import { formatDT, todayISO } from '../../utils/format';
import { PAYMENT_MODES, toOptions } from '../../utils/labels';

export default function ExpensesTab() {
  const { can } = useAuth();
  const canManage = can('FINANCE_MANAGE');
  const list = usePagedList(financeService.expenses, {});
  const [cats, setCats] = useState([]);
  const [creating, setCreating] = useState(false);
  const [cancelling, setCancelling] = useState(null);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { referenceService.expenseCategories().then(setCats).catch(() => {}); }, []);

  const fields = [
    { name: 'categorieId', label: 'Catégorie', type: 'select', numeric: true, required: true,
      options: cats.filter((c) => c.actif).map((c) => ({ value: c.id, label: c.nom })) },
    { name: 'dateDepense', label: 'Date', type: 'date', required: true },
    { name: 'libelle', label: 'Libellé', required: true, wide: true },
    { name: 'montant', label: 'Montant (DT)', type: 'number', required: true },
    { name: 'mode', label: 'Payé par', type: 'select', options: toOptions(PAYMENT_MODES), required: true, placeholder: false },
    { name: 'reference', label: 'Référence (facture, chèque…)', wide: true },
  ];

  const cancel = async () => {
    try {
      await financeService.cancelExpense(cancelling.id, motif.trim());
      setCancelling(null);
      setMotif('');
      list.reload();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <>
      <Alert type="error">{error || list.error}</Alert>
      {!cats.length && canManage ? <Alert type="info">Créez d'abord les catégories de dépenses dans Paramètres › Dépenses.</Alert> : null}
      <div className="panel">
        <Toolbar>
          <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} />
          <FilterSelect label="Catégorie" allLabel="Toutes les catégories" value={list.filters.categoryId}
                        options={cats.map((c) => ({ value: c.id, label: c.nom }))} onChange={(v) => list.updateFilter('categoryId', v)} />
          <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
          <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
          {canManage ? <Button size="sm" onClick={() => setCreating(true)} disabled={!cats.length}><Plus size={16} />Nouvelle dépense</Button> : null}
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} empty={<EmptyState icon={Receipt} title="Aucune dépense" text="Les dépenses enregistrées sortent automatiquement de la caisse." />}
                     columns={[
                       { key: 'n', header: 'N°', render: (e) => <strong>{e.numero}</strong> },
                       { key: 'd', header: 'Date', render: (e) => new Date(`${e.date}T12:00:00`).toLocaleDateString('fr-FR') },
                       { key: 'c', header: 'Catégorie', render: (e) => e.categorie },
                       { key: 'l', header: 'Libellé', render: (e) => e.libelle },
                       { key: 'm', header: 'Mode', render: (e) => PAYMENT_MODES[e.mode] },
                       { key: 'a', header: 'Montant', align: 'right', render: (e) => <strong>{formatDT(e.montant)}</strong> },
                       { key: 's', header: 'Statut', render: (e) => (e.statut === 'ANNULEE' ? <span className="badge badge-off" title={e.motifAnnulation}>Annulée</span> : <span className="badge badge-ok">Payée</span>) },
                       ...(canManage ? [{ key: 'x', header: '', align: 'right', render: (e) => (e.statut === 'VALIDEE' ? <Button size="sm" variant="danger" onClick={() => setCancelling(e)}>Annuler</Button> : null) }] : []),
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {creating ? (
        <EntityFormModal title="Nouvelle dépense" fields={fields} initial={{ dateDepense: todayISO(), mode: 'ESPECES' }}
                         onClose={() => setCreating(false)}
                         onSubmit={async (p) => { await financeService.createExpense(p); setCreating(false); list.reload(); }} />
      ) : null}
      {cancelling ? (
        <Modal title={`Annuler la dépense ${cancelling.numero}`} onClose={() => setCancelling(null)}
               footer={(<><Button variant="secondary" onClick={() => setCancelling(null)}>Ne pas annuler</Button>
                 <Button variant="danger" disabled={!motif.trim()} onClick={cancel}>Annuler la dépense</Button></>)}>
          <p style={{ marginBottom: 12 }}>Une écriture inverse de {formatDT(cancelling.montant)} sera passée en caisse.</p>
          <div className="field"><label>Motif *</label><input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} /></div>
        </Modal>
      ) : null}
    </>
  );
}
