import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Printer } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Ticket, { TICKET_FORMATS } from './Ticket';
import { salesService } from '../../services/salesService';
import { settingsService } from '../../services/settingsService';

const FORMAT_KEY = 'huilerie.ticketFormat';

/** Règle @page adaptée au format, injectée juste avant l'impression. */
function applyPageRule(format) {
  let el = document.getElementById('ticket-page-rule');
  if (!el) {
    el = document.createElement('style');
    el.id = 'ticket-page-rule';
    document.head.appendChild(el);
  }
  el.textContent = format === 'A4' ? '@page { size: A4; margin: 14mm; }' : '@page { margin: 0; }';
  document.body.classList.add('printing-ticket');
  const done = () => {
    document.body.classList.remove('printing-ticket');
    el.textContent = '';
    window.removeEventListener('afterprint', done);
  };
  window.addEventListener('afterprint', done);
}

/**
 * Aperçu + impression. Chaque impression est comptée et tracée côté serveur AVANT d'imprimer ;
 * elle ne crée jamais de vente et ne touche jamais au stock.
 */
export default function TicketPrintDialog({ sale, onClose, onPrinted }) {
  const [format, setFormat] = useState(() => localStorage.getItem(FORMAT_KEY) || '80');
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');
  const [printing, setPrinting] = useState(false);
  const [current, setCurrent] = useState(sale);

  useEffect(() => { settingsService.get().then(setSettings).catch((e) => setError(e.message)); }, []);

  const print = async () => {
    setPrinting(true);
    setError('');
    try {
      const updated = await salesService.recordPrint(current.id);
      setCurrent(updated);
      onPrinted?.(updated);
      localStorage.setItem(FORMAT_KEY, format);
      applyPageRule(format);
      // Laisse le temps au portail d'impression de se mettre à jour
      setTimeout(() => { window.print(); setPrinting(false); }, 50);
    } catch (err) {
      setError(err.message);
      setPrinting(false);
    }
  };

  const reprint = current.nbImpressions > 0;

  return (
    <>
      <Modal title={`Ticket ${current.numeroTicket}`} onClose={onClose}
             footer={(
               <>
                 <Button variant="secondary" onClick={onClose}>Fermer</Button>
                 <Button onClick={print} loading={printing}><Printer size={18} />{reprint ? 'Réimprimer' : 'Imprimer le ticket'}</Button>
               </>
             )}>
        <Alert type="error">{error}</Alert>
        {settings && !settings.configured ? (
          <Alert type="info">Les informations de l'huilerie (nom, adresse, matricule fiscal) ne sont pas encore saisies dans Paramètres.</Alert>
        ) : null}
        {reprint ? <Alert type="info">Ce ticket a déjà été imprimé {current.nbImpressions} fois : il sera marqué DUPLICATA.</Alert> : null}
        <div className="field">
          <label>Format d'impression</label>
          <select className="select" value={format} onChange={(e) => setFormat(e.target.value)}>
            {TICKET_FORMATS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
          </select>
          {format !== 'A4' ? <span className="hint">Dans la fenêtre d'impression, choisissez l'imprimante thermique et le rouleau {format} mm.</span> : null}
        </div>
        <div className="ticket-preview">
          <Ticket sale={current} settings={settings} format={format} reprint={reprint} />
        </div>
      </Modal>
      {createPortal(
        <div className="print-root">
          <Ticket sale={current} settings={settings} format={format} reprint={current.nbImpressions > 1} />
        </div>,
        document.body,
      )}
    </>
  );
}
