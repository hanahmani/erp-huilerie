import { DocHeader } from '../../components/print/DocPrintDialog';
import { formatDT, formatQty } from '../../utils/format';
import { DOC_TYPES, PAYMENT_MODES } from '../../utils/labels';

const UNITS = ['', 'un', 'deux', 'trois', 'quatre', 'cinq', 'six', 'sept', 'huit', 'neuf', 'dix', 'onze', 'douze', 'treize',
  'quatorze', 'quinze', 'seize', 'dix-sept', 'dix-huit', 'dix-neuf'];
const TENS = ['', '', 'vingt', 'trente', 'quarante', 'cinquante', 'soixante', 'soixante', 'quatre-vingt', 'quatre-vingt'];

function below1000(n) {
  if (n === 0) return '';
  const h = Math.floor(n / 100);
  const r = n % 100;
  let out = h ? (h === 1 ? 'cent' : `${UNITS[h]} cent${r === 0 ? 's' : ''}`) : '';
  if (r) {
    let t = Math.floor(r / 10);
    let u = r % 10;
    let words;
    if (r < 20) words = UNITS[r];
    else {
      if (t === 7 || t === 9) u += 10;
      words = TENS[t] + (u === 1 && t !== 8 ? ' et un' : u ? `-${UNITS[u]}` : (t === 8 ? 's' : ''));
      if (t === 7 && u === 11) words = 'soixante et onze';
    }
    out = `${out} ${words}`.trim();
  }
  return out;
}

/** Montant en toutes lettres (dinars et millimes), mention habituelle des factures tunisiennes. */
export function amountInWords(value) {
  const millimes = Math.round(Number(value) * 1000);
  const dinars = Math.floor(millimes / 1000);
  const mil = millimes % 1000;
  const words = (n) => {
    if (n === 0) return 'zéro';
    const parts = [];
    const m = Math.floor(n / 1000000);
    const k = Math.floor((n % 1000000) / 1000);
    const r = n % 1000;
    if (m) parts.push(`${below1000(m)} million${m > 1 ? 's' : ''}`);
    if (k) parts.push(k === 1 ? 'mille' : `${below1000(k)} mille`);
    if (r) parts.push(below1000(r));
    return parts.join(' ');
  };
  return `${words(dinars)} dinar${dinars > 1 ? 's' : ''}${mil ? ` et ${words(mil)} millime${mil > 1 ? 's' : ''}` : ''}`;
}

/** Document commercial imprimable (A4 conseillé ; ticket possible pour un bon de livraison). */
export default function SalesDocPrint({ d, settings, format }) {
  const showPrices = d.type !== 'LIVRAISON';
  return (
    <div className={`ticket ticket-${format}`}>
      <DocHeader settings={settings} />
      <div className="ticket-flag">{DOC_TYPES[d.type].toUpperCase()} {d.numero || '(BROUILLON)'}</div>
      <div className="ticket-meta">
        <div><span>Date</span><span>{new Date(`${d.dateDoc}T12:00:00`).toLocaleDateString('fr-FR')}</span></div>
        {d.echeance && d.type === 'FACTURE' ? <div><span>Échéance</span><span>{new Date(`${d.echeance}T12:00:00`).toLocaleDateString('fr-FR')}</span></div> : null}
        <div><span>Client</span><span>{d.clientNom} ({d.clientCode})</span></div>
        {d.clientAdresse ? <div><span>Adresse</span><span>{d.clientAdresse}</span></div> : null}
        {d.clientMatriculeFiscal ? <div><span>MF client</span><span>{d.clientMatriculeFiscal}</span></div> : null}
        {d.refClient ? <div><span>Réf. client</span><span>{d.refClient}</span></div> : null}
        {d.source ? <div><span>Origine</span><span>{DOC_TYPES[d.source.type]} {d.source.numero}</span></div> : null}
      </div>
      <table className="ticket-lines">
        <thead><tr><th>Désignation</th><th className="r">Qté</th>{showPrices ? <><th className="r">P.U. HT</th><th className="r">Rem.</th><th className="r">TVA</th><th className="r">Total HT</th></> : null}</tr></thead>
        <tbody>{d.lignes.map((l) => (
          <tr key={l.id}>
            <td>{l.designation}{l.lotNumero ? <small>lot {l.lotNumero}</small> : null}</td>
            <td className="r">{formatQty(l.quantite, l.unite)}</td>
            {showPrices ? <><td className="r">{Number(l.prixUnitaire).toFixed(3)}</td><td className="r">{Number(l.remisePct) ? `${Number(l.remisePct)}%` : ''}</td>
              <td className="r">{Number(l.tauxTva)}%</td><td className="r">{Number(l.totalHt).toFixed(3)}</td></> : null}
          </tr>
        ))}</tbody>
      </table>
      {showPrices ? (
        <div className="ticket-totals">
          <div><span>Total HT</span><span>{formatDT(d.totalHt)}</span></div>
          <div><span>Total TVA</span><span>{formatDT(d.totalTva)}</span></div>
          {Number(d.timbre) ? <div><span>Timbre fiscal</span><span>{formatDT(d.timbre)}</span></div> : null}
          <div className="ticket-total"><span>Total TTC</span><span>{formatDT(d.totalTtc)}</span></div>
          {d.paiements.map((p) => <div key={p.numero}><span>{PAYMENT_MODES[p.mode]} {p.numero}</span><span>{formatDT(p.montant)}</span></div>)}
          {d.type === 'FACTURE' && Number(d.reste) > 0 && d.paiements.length ? <div><span>Reste à payer</span><span>{formatDT(d.reste)}</span></div> : null}
        </div>
      ) : null}
      {showPrices && ['FACTURE', 'AVOIR'].includes(d.type) ? (
        <p style={{ marginTop: 8 }}>Arrêté{d.type === 'FACTURE' ? 'e la présente facture' : ' le présent avoir'} à la somme de : <strong>{amountInWords(d.totalTtc)}</strong>.</p>
      ) : null}
      {d.notes ? <p style={{ marginTop: 8 }}>{d.notes}</p> : null}
      {d.type === 'LIVRAISON' ? <footer className="ticket-foot">Reçu conforme, signature du client :<br /><br /><br /></footer> : null}
    </div>
  );
}
