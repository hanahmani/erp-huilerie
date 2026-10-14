import { Download, Wallet } from 'lucide-react';
import { usePagedList } from '../../hooks/usePagedList';
import { financeService } from '../../services/financeService';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import { dayRange, formatDateTime, formatDT, todayISO } from '../../utils/format';
import { PAYMENT_MODES, toOptions } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

export default function CashJournalTab() {
  const today = todayISO();
  const fetcher = async (p) => {
    const journal = await financeService.cash({ ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined });
    return { ...journal.lignes, totals: journal };
  };
  const list = usePagedList(fetcher, { fromDate: today, toDate: today }, 30);
  const t = list.data.totals;

  const exportAll = async () => {
    const j = await financeService.cash({ ...list.filters, ...dayRange(list.filters.fromDate, list.filters.toDate),
      fromDate: undefined, toDate: undefined, page: 0, size: 100 });
    exportCsv('journal-caisse', [
      { header: 'Date', value: (m) => formatDateTime(m.date) }, { header: 'Libellé', value: (m) => m.libelle },
      { header: 'Mode', value: (m) => PAYMENT_MODES[m.mode] },
      { header: 'Montant', value: (m) => (m.type === 'ENCAISSEMENT' ? 1 : -1) * Number(m.montant) },
      { header: 'Par', value: (m) => m.username },
    ], j.lignes.content);
  };

  return (
    <>
      <div className="stats">
        <StatCard label="Encaissements" value={t ? formatDT(t.encaissements) : '—'} />
        <StatCard label="Décaissements" value={t ? formatDT(t.decaissements) : '—'} />
        <StatCard label="Solde de la période" value={t ? formatDT(t.solde) : '—'} />
        <StatCard label="Dont espèces" value={t ? formatDT(t.especes) : '—'} />
      </div>
      <Alert type="error">{list.error}</Alert>
      <div className="panel">
        <Toolbar>
          <DateInput label="Du" value={list.filters.fromDate} onChange={(v) => list.updateFilter('fromDate', v)} />
          <DateInput label="Au" value={list.filters.toDate} onChange={(v) => list.updateFilter('toDate', v)} />
          <FilterSelect label="Type" allLabel="Entrées et sorties" value={list.filters.type}
                        options={[{ value: 'ENCAISSEMENT', label: 'Encaissements' }, { value: 'DECAISSEMENT', label: 'Décaissements' }]}
                        onChange={(v) => list.updateFilter('type', v)} />
          <FilterSelect label="Mode" allLabel="Tous les modes" value={list.filters.mode} options={toOptions(PAYMENT_MODES)}
                        onChange={(v) => list.updateFilter('mode', v)} />
          <Button variant="secondary" size="sm" onClick={exportAll}><Download size={16} />Exporter</Button>
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content}
                     empty={<EmptyState icon={Wallet} title="Aucun mouvement" text="Aucune opération de caisse sur cette période." />}
                     columns={[
                       { key: 'd', header: 'Date', render: (m) => formatDateTime(m.date) },
                       { key: 'l', header: 'Libellé', render: (m) => <strong>{m.libelle}</strong> },
                       { key: 'm', header: 'Mode', render: (m) => PAYMENT_MODES[m.mode] },
                       { key: 'a', header: 'Montant', align: 'right', render: (m) => <strong style={{ color: m.type === 'ENCAISSEMENT' ? 'var(--success)' : 'var(--danger)' }}>{m.type === 'ENCAISSEMENT' ? '+' : '−'} {formatDT(m.montant)}</strong> },
                       { key: 'u', header: 'Par', render: (m) => <span className="muted">{m.username}</span> },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
    </>
  );
}
