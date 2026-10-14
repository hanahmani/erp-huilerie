import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Printer } from 'lucide-react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Alert from '../common/Alert';
import { settingsService } from '../../services/settingsService';
import { TICKET_FORMATS } from '../../pages/sales/Ticket';

/** Prépare la page pour imprimer uniquement le document, au format choisi. */
export function preparePrint(format) {
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

/** En-tête commun aux bons imprimés : informations saisies dans les paramètres, rien d'inventé. */
export function DocHeader({ settings }) {
  const s = settings || {};
  return (
    <header className="ticket-head">
      <img src="/logo/logo.png" alt="" className="ticket-logo" onError={(e) => { e.currentTarget.style.display = 'none'; }} />
      {s.raisonSociale ? <div className="ticket-company">{s.raisonSociale}</div> : null}
      {s.adresse ? <div>{s.adresse}</div> : null}
      {s.telephone ? <div>Tél. {s.telephone}</div> : null}
      {s.matriculeFiscal ? <div>MF : {s.matriculeFiscal}</div> : null}
    </header>
  );
}

/** Aperçu + impression d'un bon ; render(format, settings) renvoie le contenu du document. */
export default function DocPrintDialog({ title, render, onClose }) {
  const [format, setFormat] = useState(() => localStorage.getItem('huilerie.ticketFormat') || '80');
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { settingsService.get().then(setSettings).catch((e) => setError(e.message)); }, []);

  const print = () => {
    localStorage.setItem('huilerie.ticketFormat', format);
    preparePrint(format);
    setTimeout(() => window.print(), 50);
  };

  return (
    <>
      <Modal title={title} onClose={onClose}
             footer={(<><Button variant="secondary" onClick={onClose}>Fermer</Button><Button onClick={print}><Printer size={18} />Imprimer</Button></>)}>
        <Alert type="error">{error}</Alert>
        <div className="field">
          <label>Format d'impression</label>
          <select className="select" value={format} onChange={(e) => setFormat(e.target.value)}>
            {TICKET_FORMATS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
          </select>
        </div>
        <div className="ticket-preview">{render(format, settings)}</div>
      </Modal>
      {createPortal(<div className="print-root">{render(format, settings)}</div>, document.body)}
    </>
  );
}
