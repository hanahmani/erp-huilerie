export default function StatusBadge({ active, on = 'Actif', off = 'Inactif' }) {
  return <span className={`badge ${active ? 'badge-ok' : 'badge-off'}`}>{active ? on : off}</span>;
}
