import { formatDT, formatQty } from '../../utils/format';
import { PAYMENT_MODES } from '../../utils/labels';

export const TICKET_FORMATS = [
  { value: '80', label: 'Ticket thermique 80 mm' },
  { value: '58', label: 'Ticket thermique 58 mm' },
  { value: 'A4', label: 'Ticket client A4' },
];

const dateTime = (iso) => new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
}).format(new Date(iso));

/**
 * Ticket de vente. Toutes les informations viennent de la vente enregistrée et des paramètres :
 * rien n'est inventé, et une information légale absente n'est pas affichée.
 */
export default function Ticket({ sale, settings, format = '80', reprint }) {
  const s = settings || {};
  return (
    <div className={`ticket ticket-${format}`}>
      <header className="ticket-head">
        <img src="/logo/logo.png" alt="" className="ticket-logo" onError={(e) => { e.currentTarget.style.display = 'none'; }} />
        {s.raisonSociale ? <div className="ticket-company">{s.raisonSociale}</div> : null}
        {s.adresse ? <div>{s.adresse}</div> : null}
        {s.telephone ? <div>Tél. {s.telephone}</div> : null}
        {s.matriculeFiscal ? <div>MF : {s.matriculeFiscal}</div> : null}
      </header>

      <div className="ticket-meta">
        <div><span>Ticket</span><strong>{sale.numeroTicket}</strong></div>
        <div><span>Date</span><span>{dateTime(sale.date)}</span></div>
        <div><span>Caissier</span><span>{sale.caissierNom}</span></div>
        <div><span>Client</span><span>{sale.clientNom || 'Client comptoir'}</span></div>
      </div>
      {reprint ? <div className="ticket-flag">DUPLICATA</div> : null}
      {sale.statut === 'ANNULEE' ? <div className="ticket-flag">VENTE ANNULÉE</div> : null}

      <table className="ticket-lines">
        <thead>
          <tr><th>Produit</th><th className="r">Qté</th><th className="r">P.U.</th><th className="r">Total</th></tr>
        </thead>
        <tbody>
          {sale.lignes.map((l, i) => (
            <tr key={i}>
              <td>{l.designation}{l.lotNumero ? <small> lot {l.lotNumero}</small> : null}</td>
              <td className="r">{formatQty(l.quantite, l.unite)}</td>
              <td className="r">{Number(l.prixUnitaire).toFixed(3)}</td>
              <td className="r">{Number(l.total).toFixed(3)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="ticket-totals">
        <div><span>Sous-total</span><span>{formatDT(sale.sousTotal)}</span></div>
        {Number(sale.remise) > 0 ? <div><span>Remise</span><span>− {formatDT(sale.remise)}</span></div> : null}
        <div className="ticket-total"><span>TOTAL</span><span>{formatDT(sale.total)}</span></div>
        {sale.paiements.map((p, i) => (
          <div key={i}><span>{PAYMENT_MODES[p.mode]}</span><span>{formatDT(p.montant)}</span></div>
        ))}
        {Number(sale.monnaieRendue) > 0 ? (
          <>
            <div><span>Espèces reçues</span><span>{formatDT(Number(sale.montantRecu) - sale.paiements.filter((p) => p.mode !== 'ESPECES').reduce((a, p) => a + Number(p.montant), 0))}</span></div>
            <div><span>Monnaie rendue</span><span>{formatDT(sale.monnaieRendue)}</span></div>
          </>
        ) : null}
        <div><span>Montant payé</span><span>{formatDT(sale.montantPaye)}</span></div>
        {Number(sale.resteAPayer) > 0 ? <div className="ticket-total"><span>Reste à payer</span><span>{formatDT(sale.resteAPayer)}</span></div> : null}
      </div>

      <footer className="ticket-foot">{s.messageTicket || 'Merci de votre visite'}</footer>
    </div>
  );
}
