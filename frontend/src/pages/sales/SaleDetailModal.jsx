import { Fragment, useEffect, useState } from 'react';
import { Ban, Printer } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import { useAuth } from '../../context/AuthContext';
import { salesService } from '../../services/salesService';
import { formatDateTime, formatDT, formatQty } from '../../utils/format';
import { PAYMENT_MODES, PAYMENT_STATUS } from '../../utils/labels';
import TicketPrintDialog from './TicketPrintDialog';

export default function SaleDetailModal({ saleId, onClose, onChanged }) {
  const { can } = useAuth();
  const [sale, setSale] = useState(null);
  const [error, setError] = useState('');
  const [printing, setPrinting] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [motif, setMotif] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { salesService.get(saleId).then(setSale).catch((e) => setError(e.message)); }, [saleId]);

  const cancel = async () => {
    setBusy(true);
    setError('');
    try {
      const s = await salesService.cancel(sale.id, motif.trim());
      setSale(s);
      setCancelling(false);
      onChanged?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  if (printing) {
    return <TicketPrintDialog sale={sale} onClose={() => setPrinting(false)} onPrinted={(s) => { setSale(s); onChanged?.(); }} />;
  }

  const active = sale?.statut === 'VALIDEE';
  return (
    <Modal title={sale ? `Vente ${sale.numeroTicket}` : 'Vente'} onClose={onClose} wide
           footer={sale ? (
             <>
               {active && can('SALES_CANCEL') && !cancelling
                 ? <Button variant="danger" onClick={() => setCancelling(true)}><Ban size={16} />Annuler la vente</Button> : null}
               <Button variant="secondary" onClick={onClose}>Fermer</Button>
               <Button onClick={() => setPrinting(true)}><Printer size={16} />{sale.nbImpressions ? 'Réimprimer' : 'Imprimer'}</Button>
             </>
           ) : null}>
      <Alert type="error">{error}</Alert>
      {!sale ? <Spinner /> : (
        <>
          {sale.statut === 'ANNULEE' ? (
            <Alert type="error">Annulée le {formatDateTime(sale.annuleeLe)} par {sale.annuleePar} : {sale.motifAnnulation}</Alert>
          ) : null}
          <dl className="kv" style={{ marginBottom: 18 }}>
            <dt>Date</dt><dd>{formatDateTime(sale.date)}</dd>
            <dt>Caissier</dt><dd>{sale.caissierNom}</dd>
            <dt>Client</dt><dd>{sale.clientNom || 'Client comptoir'}</dd>
            <dt>Paiement</dt><dd>{PAYMENT_STATUS[sale.paiementStatut]}</dd>
            <dt>Impressions</dt><dd>{sale.nbImpressions}</dd>
          </dl>
          <div className="table-wrap" style={{ border: '1px solid var(--beige-300)', borderRadius: 8 }}>
            <table className="table">
              <thead><tr><th>Produit</th><th>Lot</th><th className="num">Quantité</th><th className="num">Prix unitaire</th><th className="num">Total</th></tr></thead>
              <tbody>
                {sale.lignes.map((l, i) => (
                  <tr key={i}>
                    <td>{l.designation}</td><td>{l.lotNumero || '—'}</td>
                    <td className="num">{formatQty(l.quantite, l.unite)}</td>
                    <td className="num">{formatDT(l.prixUnitaire)}</td>
                    <td className="num">{formatDT(l.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <dl className="kv" style={{ marginTop: 18, justifyContent: 'end' }}>
            <dt>Sous-total</dt><dd>{formatDT(sale.sousTotal)}</dd>
            {Number(sale.remise) > 0 ? <><dt>Remise</dt><dd>− {formatDT(sale.remise)}</dd></> : null}
            <dt>Total</dt><dd><strong>{formatDT(sale.total)}</strong></dd>
            {sale.paiements.map((p, i) => <Fragment key={i}><dt>{PAYMENT_MODES[p.mode]}</dt><dd>{formatDT(p.montant)}</dd></Fragment>)}
            {Number(sale.monnaieRendue) > 0 ? <><dt>Monnaie rendue</dt><dd>{formatDT(sale.monnaieRendue)}</dd></> : null}
            <dt>Reste à payer</dt><dd>{formatDT(sale.resteAPayer)}</dd>
          </dl>
          {cancelling ? (
            <div className="panel" style={{ marginTop: 18, padding: 16 }}>
              <p style={{ marginBottom: 10 }}>
                L'annulation remet les produits en stock et enregistre un décaissement de {formatDT(sale.montantPaye)}. Elle est définitive et tracée.
              </p>
              <div className="field">
                <label>Motif d'annulation *</label>
                <input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} maxLength={255} autoFocus />
              </div>
              <div className="actions">
                <Button variant="secondary" onClick={() => setCancelling(false)}>Ne pas annuler</Button>
                <Button variant="danger" loading={busy} disabled={!motif.trim()} onClick={cancel}>Confirmer l'annulation</Button>
              </div>
            </div>
          ) : null}
        </>
      )}
    </Modal>
  );
}
