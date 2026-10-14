import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { dashboardService } from '../../services/dashboardService';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import StatCard from '../../components/common/StatCard';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';

/** Indicateurs calculés par le serveur à partir des opérations enregistrées. */
function WeekChart({ days }) {
  const max = Math.max(...days.map((d) => Number(d.total)), 0);
  const label = (iso) => new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(new Date(`${iso}T12:00:00`));
  return (
    <div className="week-chart" role="img" aria-label="Ventes des 7 derniers jours">
      {days.map((d) => (
        <div key={d.jour} className="week-bar">
          <span className="week-value">{Number(d.total) ? Number(d.total).toFixed(0) : ''}</span>
          <div className="week-fill" style={{ height: max ? `${(Number(d.total) / max) * 100}%` : '0%' }} />
          <span className="week-day">{label(d.jour)}</span>
        </div>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { dashboardService.get().then(setData).catch((e) => setError(e.message)); }, []);
  const hour = new Date().getHours();

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="dash-title">{hour < 18 ? 'Bonjour' : 'Bonsoir'}, {user.firstName}</h1>
          <p>Voici l'activité de l'huilerie aujourd'hui.</p>
        </div>
      </div>
      <Alert type="error">{error}</Alert>
      {!data && !error ? <Spinner /> : null}
      {data ? (
        <>
          <div className="stats">
            {data.ventesJour != null ? <StatCard label="Ventes du jour" value={formatDT(data.ventesJour)} hint={`${data.nbVentesJour} ticket${data.nbVentesJour > 1 ? 's' : ''}`} /> : null}
            {data.ventesMois != null ? <StatCard label="Ventes du mois" value={formatDT(data.ventesMois)} /> : null}
            {data.encaissementsJour != null ? <StatCard label="Caisse du jour (net)" value={formatDT(data.encaissementsJour)} /> : null}
            {data.creancesClients != null ? <StatCard label="Reste dû par les clients" value={formatDT(data.creancesClients)} tone={Number(data.creancesClients) > 0 ? 'warn' : undefined} /> : null}
            {data.alertesStock != null ? <StatCard label="Articles en stock faible" value={data.alertesStock.length} tone={data.alertesStock.length ? 'warn' : undefined} /> : null}
            {data.nbLotsBloques != null ? <StatCard label="Lots bloqués" value={data.nbLotsBloques} tone={data.nbLotsBloques ? 'warn' : undefined} /> : null}
            {data.pressagesEnCours != null ? <StatCard label="Pressages en cours" value={data.pressagesEnCours} /> : null}
            {data.huileClientsARemettre != null ? <StatCard label="Huile des clients à remettre" value={formatQty(data.huileClientsARemettre, 'L')} tone={Number(data.huileClientsARemettre) ? 'warn' : undefined} /> : null}
            {data.pressagesImpayes != null && Number(data.pressagesImpayes) ? <StatCard label="Pressages non payés" value={formatDT(data.pressagesImpayes)} tone="warn" /> : null}
            {data.huileProduiteMois != null ? <StatCard label="Huile produite ce mois" value={formatQty(data.huileProduiteMois, 'L')} hint={data.rendementMois != null ? `Rendement ${Number(data.rendementMois).toFixed(2)} L/100 kg` : undefined} /> : null}
            {data.triturationsEnCours != null && data.triturationsEnCours ? <StatCard label="Triturations à faire ou en cours" value={data.triturationsEnCours} /> : null}
            {data.achatsMois != null ? <StatCard label="Achats du mois" value={formatDT(data.achatsMois)} /> : null}
            {data.commandesARecevoir != null ? <StatCard label="Commandes à réceptionner" value={data.commandesARecevoir} tone={data.commandesARecevoir ? 'warn' : undefined} /> : null}
            {data.dettesFournisseurs != null ? <StatCard label="Dû aux fournisseurs" value={formatDT(data.dettesFournisseurs)} tone={Number(data.dettesFournisseurs) ? 'warn' : undefined} /> : null}
            {data.depensesMois != null ? <StatCard label="Dépenses du mois" value={formatDT(data.depensesMois)} /> : null}
            {data.caisseOuverte != null ? (
              <StatCard label="Caisse" value={data.caisseOuverte ? formatDT(data.especesEnCaisse) : 'Fermée'}
                        hint={data.caisseOuverte ? 'Espèces attendues dans le tiroir' : 'Ouvrez-la dans Finance et caisse'}
                        tone={data.caisseOuverte ? undefined : 'warn'} />
            ) : null}
          </div>

          <div className="grid-2">
            {data.ventes7Jours ? (
              <div className="panel">
                <div className="section-title"><h2>Ventes des 7 derniers jours (DT)</h2></div>
                <div className="panel-body"><WeekChart days={data.ventes7Jours} /></div>
              </div>
            ) : null}

            {data.alertesStock ? (
              <div className="panel">
                <div className="section-title"><h2>Stock faible</h2><Link to="/stock">Voir le stock</Link></div>
                {data.alertesStock.length ? (
                  <table className="table"><tbody>
                    {data.alertesStock.slice(0, 6).map((a) => (
                      <tr key={a.articleId}>
                        <td><strong>{a.nom}</strong></td>
                        <td className="num">{formatQty(a.disponible, a.unite)}</td>
                        <td className="num muted">seuil {formatQty(a.seuilMin, a.unite)}</td>
                      </tr>
                    ))}
                  </tbody></table>
                ) : <div className="panel-body muted">Aucun article sous son seuil d'alerte.</div>}
              </div>
            ) : null}

            {data.dernieresVentes ? (
              <div className="panel">
                <div className="section-title"><h2>Dernières ventes</h2><Link to="/ventes/historique">Historique</Link></div>
                {data.dernieresVentes.length ? (
                  <table className="table"><tbody>
                    {data.dernieresVentes.map((s) => (
                      <tr key={s.id}>
                        <td><strong>{s.numeroTicket}</strong><div className="muted" style={{ fontSize: 12 }}>{formatDateTime(s.date)}</div></td>
                        <td>{s.clientNom || <span className="muted">Comptoir</span>}</td>
                        <td className="num">{s.statut === 'ANNULEE' ? <span className="badge badge-off">Annulée</span> : formatDT(s.total)}</td>
                      </tr>
                    ))}
                  </tbody></table>
                ) : <div className="panel-body muted">Aucune vente enregistrée pour l'instant.</div>}
              </div>
            ) : null}

            <div className="panel">
              <div className="section-title"><h2>Référentiels</h2></div>
              <div className="panel-body">
                <dl className="kv">
                  {data.nbArticles != null ? <><dt>Articles actifs</dt><dd><Link to="/catalogue">{data.nbArticles}</Link></dd></> : null}
                  {data.nbClients != null ? <><dt>Clients actifs</dt><dd><Link to="/clients">{data.nbClients}</Link></dd></> : null}
                  {data.nbFournisseurs != null ? <><dt>Fournisseurs actifs</dt><dd><Link to="/fournisseurs">{data.nbFournisseurs}</Link></dd></> : null}
                </dl>
              </div>
            </div>
          </div>
        </>
      ) : null}
    </>
  );
}
