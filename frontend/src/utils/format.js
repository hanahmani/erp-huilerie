// Formatage commun. Le dinar tunisien s'affiche avec 3 décimales (millimes).
const dtFormatter = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });

export function formatDT(value) {
  if (value === null || value === undefined || value === '') return '—';
  return `${dtFormatter.format(Number(value))} DT`;
}

const DEVISES = { TND: { decimals: 3, symbol: 'DT' }, EUR: { decimals: 2, symbol: '€' }, USD: { decimals: 2, symbol: '$' } };

/** Montant dans la devise du document (TND : 3 décimales, EUR / USD : 2). */
export function formatMoney(value, devise = 'TND') {
  if (value === null || value === undefined || value === '') return '—';
  const d = DEVISES[devise] || DEVISES.TND;
  const f = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: d.decimals, maximumFractionDigits: d.decimals });
  return `${f.format(Number(value))} ${d.symbol}`;
}

export function formatDateTime(value) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short', hour12: false }).format(new Date(value));
}

export function formatToday() {
  return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date());
}

export function initials(user) {
  if (!user) return '';
  return `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();
}

export function formatQty(value, unit) {
  if (value === null || value === undefined) return '—';
  const n = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 3 }).format(Number(value));
  return unit ? `${n} ${unit}` : n;
}

/** Bornes ISO d'une période saisie en dates locales (fin exclue = lendemain 00:00). */
export function dayRange(fromDate, toDate) {
  const from = fromDate ? new Date(`${fromDate}T00:00:00`).toISOString() : undefined;
  let to;
  if (toDate) {
    const d = new Date(`${toDate}T00:00:00`);
    d.setDate(d.getDate() + 1);
    to = d.toISOString();
  }
  return { from, to };
}

export function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const pad2 = (n) => String(n).padStart(2, '0');

/** Date ISO (ou maintenant si absente) → valeur d'un champ datetime-local, à l'heure locale du poste. */
export function toLocalInput(iso) {
  const d = iso ? new Date(iso) : new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/** Valeur d'un champ datetime-local → date ISO. */
export const fromLocalInput = (v) => (v ? new Date(v).toISOString() : null);

/**
 * Rendement d'un pressage (huile ÷ olives nettes), à partir du rendement « pour 100 kg » renvoyé par le serveur :
 * 1000 kg d'olives, 200 kg d'huile → « 0,200 · 20,00 % ».
 */
export function formatYield(rendement) {
  if (rendement == null) return '—';
  const pct = Number(rendement);
  const ratio = (pct / 100).toLocaleString('fr-FR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
  return `${ratio} · ${pct.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} %`;
}
