import { DocHeader } from '../../components/print/DocPrintDialog';
import { formatDT, formatQty } from '../../utils/format';
import { OLIVE_STATES, PAYMENT_MODES } from '../../utils/labels';

const dt = (iso) => new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(iso));
const Row = ({ k, v, strong }) => (v === null || v === undefined || v === '' ? null : <div className={strong ? 'ticket-total' : undefined}><span>{k}</span><span>{v}</span></div>);

/** Bon de réception / pesée remis au client à l'arrivée des olives. */
export function ReceptionTicket({ p, settings, format }) {
  return (
    <div className={`ticket ticket-${format}`}>
      <DocHeader settings={settings} />
      <div className="ticket-flag">BON DE PESÉE — PRESSAGE</div>
      <div className="ticket-meta">
        <Row k="N°" v={p.numero} />
        <Row k="Date" v={dt(p.dateReception)} />
        <Row k="Client" v={p.clientNom} />
        <Row k="Code" v={p.clientCode} />
        <Row k="Variété" v={p.variete} />
        <Row k="Provenance" v={p.provenance} />
        <Row k="État" v={OLIVE_STATES[p.etatOlives]} />
      </div>
      <div className="ticket-totals">
        <Row k="Poids brut" v={formatQty(p.poidsBrut, 'kg')} />
        <Row k="Tare" v={formatQty(p.tare, 'kg')} />
        <Row k="Poids net" v={formatQty(p.poidsNet, 'kg')} strong />
        <Row k="Tarif" v={`${formatDT(p.tarifKg)} / kg`} />
        <Row k="Montant du service" v={formatDT(p.montant)} strong />
        <Row k="Payé" v={formatDT(p.montantPaye)} />
        {Number(p.reste) > 0 ? <Row k="Reste à payer" v={formatDT(p.reste)} /> : null}
      </div>
      <footer className="ticket-foot">Reçu par {p.recuPar}. Conservez ce bon pour retirer votre huile.</footer>
    </div>
  );
}

/** Bon de remise : huile obtenue, quantités déjà remises, paiements. */
export function ReturnTicket({ p, settings, format }) {
  return (
    <div className={`ticket ticket-${format}`}>
      <DocHeader settings={settings} />
      <div className="ticket-flag">BON DE REMISE D'HUILE</div>
      <div className="ticket-meta">
        <Row k="Pressage" v={p.numero} />
        <Row k="Client" v={p.clientNom} />
        <Row k="Reçu le" v={dt(p.dateReception)} />
        <Row k="Lot" v={p.lotNumero} />
      </div>
      <div className="ticket-totals">
        <Row k="Olives (net)" v={formatQty(p.poidsNet, 'kg')} />
        <Row k="Huile obtenue" v={formatQty(p.huileObtenue, p.unite)} strong />
        <Row k="Rendement" v={p.rendement != null ? `${Number(p.rendement).toFixed(2)} ${p.unite}/100 kg` : null} />
      </div>
      <table className="ticket-lines">
        <thead><tr><th>Remise</th><th className="r">Qté</th></tr></thead>
        <tbody>{p.remises.map((r) => (
          <tr key={r.numero}><td>{r.numero}<small>{dt(r.date)} · {r.remisA}</small></td><td className="r">{formatQty(r.quantite, p.unite)}</td></tr>
        ))}</tbody>
      </table>
      <div className="ticket-totals">
        <Row k="Total remis" v={formatQty(p.huileRemise, p.unite)} strong />
        {Number(p.huileARemettre) > 0 ? <Row k="Reste à remettre" v={formatQty(p.huileARemettre, p.unite)} /> : null}
        <Row k="Service" v={formatDT(p.montant)} />
        {p.paiements.map((x) => <Row key={x.numero} k={PAYMENT_MODES[x.mode]} v={formatDT(x.montant)} />)}
        {Number(p.reste) > 0 ? <Row k="Reste à payer" v={formatDT(p.reste)} strong /> : null}
      </div>
      <footer className="ticket-foot">Signature du client :<br /><br /><br /></footer>
    </div>
  );
}
