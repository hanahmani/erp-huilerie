/**
 * Export CSV lisible par Excel (séparateur « ; », BOM UTF-8, décimales avec virgule).
 * columns : [{ header, value: (row) => string|number }]
 */
export function exportCsv(filename, columns, rows) {
  const esc = (v) => {
    if (v === null || v === undefined) return '';
    let s = typeof v === 'number' ? String(v).replace('.', ',') : String(v);
    if (/^-?\d+\.\d+$/.test(s)) s = s.replace('.', ',');
    return /[;"\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const lines = [columns.map((c) => esc(c.header)).join(';'), ...rows.map((r) => columns.map((c) => esc(c.value(r))).join(';'))];
  const blob = new Blob([`\uFEFF${lines.join('\r\n')}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
