// Calculs d'affichage en millimes entiers pour éviter les erreurs des nombres flottants.
// Le backend refait tous les calculs : ceux-ci servent uniquement à l'aperçu en caisse.
export const toMillimes = (v) => {
  if (v === '' || v === null || v === undefined) return 0;
  const n = Number(String(v).replace(',', '.'));
  return Number.isFinite(n) ? Math.round(n * 1000) : 0;
};
export const fromMillimes = (m) => (m / 1000).toFixed(3);
export const lineMillimes = (qty, price) => Math.round((toMillimes(qty) * toMillimes(price)) / 1000);
export const normalizeDecimal = (v) => String(v ?? '').replace(',', '.').trim();
