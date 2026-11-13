import { DocHeader } from '../../components/print/DocPrintDialog';
import { formatDT, formatQty } from '../../utils/format';
import { OLIVE_STATES, PRESSING_MODES } from '../../utils/labels';

/** Formats proposés pour le bulletin. */
export const BULLETIN_FORMATS = ['A4', '80'];

const dt = (iso) => new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(iso));

/** Ligne du bulletin ; une valeur absente devient une ligne pointillée à compléter à la main. */
function Field({ k, v, strong }) {
  const empty = v === null || v === undefined || v === '';
  return (
    <div className={`bulletin-field${strong ? ' ticket-total' : ''}`}>
      <span>{k}</span>
      {empty ? <span className="bulletin-blank" aria-label="à compléter" /> : <span>{v}</span>}
    </div>
  );
}

function Block({ title, children }) {
  return (
    <section className="bulletin-block">
      <h3>{title}</h3>
      {children}
    </section>
  );
}

/** Bulletin de pressage, rempli à l'arrivée des olives du client. */
export default function BulletinPressage({ p, settings, format }) {
  const achat = p.mode === 'ACHAT';
  return (
    <div className={`ticket ticket-${format} bulletin`}>
      <DocHeader settings={settings} />
      <div className="ticket-flag">BULLETIN DE PRESSAGE N° {p.numeroBulletin}</div>
      <div className="bulletin-date">Reçu le {dt(p.dateReception)} · Pressage {p.numero}{p.numeroCarnet ? ` · Carnet n° ${p.numeroCarnet}` : ''}</div>
      <div className="bulletin-date"><strong>{PRESSING_MODES[p.mode] || PRESSING_MODES.SERVICE}</strong></div>

      <div className="bulletin-grid">
        <Block title={achat ? 'Vendeur' : 'Client'}>
          <Field k="N° client" v={p.clientCode} />
          <Field k="Nom" v={p.clientNom} />
          <Field k="Adresse" v={p.adresse} />
        </Block>
        <Block title="Transport">
          <Field k="N° bon de livraison" v={p.blNumero} />
          <Field k="Transporteur" v={p.transporteur} />
          <Field k="N° véhicule" v={p.vehicule} />
        </Block>
      </div>

      <Block title="Olives">
        <Field k="Variété" v={p.variete} />
        <Field k="Provenance" v={p.provenance} />
        <Field k="État" v={OLIVE_STATES[p.etatOlives]} />
        <Field k="Poids brut" v={formatQty(p.poidsBrut, 'kg')} />
        <Field k="Tare" v={formatQty(p.tare, 'kg')} />
        <Field k="Poids net" v={formatQty(p.poidsNet, 'kg')} strong />
        {achat ? (
          <>
            <Field k="Prix d'achat" v={`${formatDT(p.prixOlivesKg)} / kg`} />
            <Field k="Montant à payer au vendeur" v={formatDT(p.montantAchat)} strong />
          </>
        ) : (
          <>
            <Field k="Tarif" v={`${formatDT(p.tarifKg)} / kg`} />
            <Field k="Montant des frais" v={formatDT(p.montant)} strong />
          </>
        )}
      </Block>

      <div className="bulletin-signatures">
        <div><span>{achat ? 'Le vendeur' : 'Le client'} / Le transporteur</span></div>
        <div><span>L'huilerie</span></div>
      </div>
    </div>
  );
}
