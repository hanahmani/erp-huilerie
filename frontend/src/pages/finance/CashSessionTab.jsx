import { useCallback, useEffect, useState } from 'react';
import { DoorClosed, DoorOpen } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { financeService } from '../../services/financeService';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/tables/DataTable';
import EmptyState from '../../components/common/EmptyState';
import { formatDateTime, formatDT } from '../../utils/format';
import { normalizeDecimal, toMillimes, fromMillimes } from '../../utils/money';

export default function CashSessionTab() {
  const { can } = useAuth();
  const canManage = can('CASH_SESSION');
  const [session, setSession] = useState(undefined);
  const [history, setHistory] = useState([]);
  const [fond, setFond] = useState('');
  const [compte, setCompte] = useState('');
  const [justification, setJustification] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setSession(await financeService.session());
      setHistory((await financeService.sessions({ size: 10 })).content);
    } catch (e) {
      setError(e.message);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try { await fn(); setFond(''); setCompte(''); setJustification(''); await load(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };

  if (session === undefined) return <Spinner />;
  const ecartM = session && compte !== '' ? toMillimes(compte) - toMillimes(session.especesTheoriques) : 0;

  return (
    <>
      <Alert type="error">{error}</Alert>
      {session ? (
        <>
          <Alert type="success">Caisse ouverte par {session.ouvertePar} le {formatDateTime(session.ouverteLe)}.</Alert>
          <div className="stats">
            <StatCard label="Fond initial" value={formatDT(session.fondInitial)} />
            <StatCard label="Entrées en espèces" value={formatDT(session.entreesEspeces)} />
            <StatCard label="Sorties en espèces" value={formatDT(session.sortiesEspeces)} />
            <StatCard label="Espèces attendues dans le tiroir" value={formatDT(session.especesTheoriques)} />
          </div>
          {canManage ? (
            <div className="panel" style={{ maxWidth: 620 }}>
              <div className="panel-body">
                <h2 style={{ marginBottom: 12 }}>Fermer la caisse</h2>
                <div className="field"><label>Espèces comptées (DT)</label>
                  <input className="input" inputMode="decimal" value={compte} onChange={(e) => setCompte(e.target.value)} /></div>
                {compte !== '' && ecartM !== 0 ? (
                  <>
                    <Alert type="error">Écart de {formatDT(fromMillimes(ecartM))} par rapport au montant attendu.</Alert>
                    <div className="field"><label>Justification de l'écart *</label>
                      <input className="input" value={justification} onChange={(e) => setJustification(e.target.value)} maxLength={255} /></div>
                  </>
                ) : null}
                <Button loading={busy} disabled={compte === '' || (ecartM !== 0 && !justification.trim())}
                        onClick={() => run(() => financeService.closeSession(normalizeDecimal(compte), justification || null))}>
                  <DoorClosed size={18} />Fermer la caisse
                </Button>
              </div>
            </div>
          ) : null}
        </>
      ) : (
        <div className="panel" style={{ maxWidth: 620 }}>
          <div className="panel-body">
            <h2 style={{ marginBottom: 6 }}>Aucune caisse ouverte</h2>
            <p className="muted" style={{ marginBottom: 14 }}>Ouvrez la caisse avec le fond présent dans le tiroir pour suivre les espèces de la journée.</p>
            {canManage ? (
              <>
                <div className="field"><label>Fond de caisse initial (DT)</label>
                  <input className="input" inputMode="decimal" value={fond} onChange={(e) => setFond(e.target.value)} placeholder="0,000" /></div>
                <Button loading={busy} onClick={() => run(() => financeService.openSession(normalizeDecimal(fond) || '0'))}>
                  <DoorOpen size={18} />Ouvrir la caisse
                </Button>
              </>
            ) : null}
          </div>
        </div>
      )}
      <div className="panel" style={{ marginTop: 20 }}>
        <div className="section-title"><h2>Dernières sessions</h2></div>
        <DataTable rows={history} empty={<EmptyState title="Aucune session" text="L'historique des ouvertures et fermetures apparaîtra ici." />}
                   columns={[
                     { key: 'o', header: 'Ouverture', render: (s) => `${formatDateTime(s.ouverteLe)} · ${s.ouvertePar}` },
                     { key: 'f', header: 'Fermeture', render: (s) => (s.ouverte ? <span className="badge badge-ok">Ouverte</span> : `${formatDateTime(s.fermeeLe)} · ${s.fermeePar}`) },
                     { key: 'fi', header: 'Fond', align: 'right', render: (s) => formatDT(s.fondInitial) },
                     { key: 't', header: 'Attendu', align: 'right', render: (s) => formatDT(s.especesTheoriques) },
                     { key: 'c', header: 'Compté', align: 'right', render: (s) => (s.especesComptees != null ? formatDT(s.especesComptees) : '—') },
                     { key: 'e', header: 'Écart', align: 'right', render: (s) => (s.ecart == null ? '—' : <span title={s.justification || ''} style={{ color: Number(s.ecart) ? 'var(--danger)' : undefined }}>{formatDT(s.ecart)}</span>) },
                   ]} />
      </div>
    </>
  );
}
