import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Download, Truck } from 'lucide-react';
import { purchaseService } from '../../services/purchaseService';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/tables/DataTable';
import { formatDT } from '../../utils/format';
import { toMillimes } from '../../utils/money';
import { exportCsv } from '../../utils/exportCsv';

export default function PayablesTab() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { purchaseService.payables().then(setRows).catch((e) => setError(e.message)); }, []);
  if (!rows) return error ? <Alert type="error">{error}</Alert> : <Spinner />;
  const total = rows.reduce((s, r) => s + toMillimes(r.reste), 0) / 1000;

  return (
    <>
      <div className="stats">
        <StatCard label="Total dû aux fournisseurs" value={formatDT(total)} tone={total ? 'warn' : undefined} />
        <StatCard label="Fournisseurs concernés" value={rows.length} />
      </div>
      <div className="panel">
        <div className="section-title"><h2>Dettes fournisseurs</h2>
          <Button size="sm" variant="secondary" onClick={() => exportCsv('dettes-fournisseurs', [
            { header: 'Code', value: (r) => r.code }, { header: 'Fournisseur', value: (r) => r.nom },
            { header: 'Commandes', value: (r) => r.nbCommandes }, { header: 'Reste dû', value: (r) => Number(r.reste) },
          ], rows)}><Download size={16} />Exporter</Button></div>
        <DataTable rows={rows} rowKey="fournisseurId" empty={<EmptyState icon={Truck} title="Aucune dette" text="Toutes les commandes validées sont soldées." />}
                   columns={[
                     { key: 'n', header: 'Fournisseur', render: (r) => <><strong>{r.nom}</strong><div className="muted" style={{ fontSize: 12 }}>{r.code}</div></> },
                     { key: 'c', header: 'Commandes non soldées', align: 'right', render: (r) => r.nbCommandes },
                     { key: 'r', header: 'Reste dû', align: 'right', render: (r) => <strong>{formatDT(r.reste)}</strong> },
                     { key: 'x', header: '', align: 'right', render: () => <Link to="/achats" className="btn btn-secondary btn-sm">Voir les achats</Link> },
                   ]} />
      </div>
    </>
  );
}
