// Formatage commun. Le dinar tunisien s'affiche avec 3 décimales (millimes).
const dtFormatter = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });

export function formatDT(value) {
  if (value === null || value === undefined || value === '') return '—';
  return `${dtFormatter.format(Number(value))} DT`;
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
