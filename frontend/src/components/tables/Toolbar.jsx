import { Search } from 'lucide-react';

/** Barre de filtres au-dessus d'un tableau. */
export function Toolbar({ children }) {
  return <div className="toolbar">{children}</div>;
}

export function SearchBox({ value, onChange, placeholder = 'Rechercher…' }) {
  return (
    <label className="search-box">
      <Search size={16} />
      <input type="search" value={value ?? ''} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

export function FilterSelect({ value, onChange, options, allLabel = 'Tous', label }) {
  return (
    <select className="select select-inline" value={value ?? ''} aria-label={label}
            onChange={(e) => onChange(e.target.value)}>
      <option value="">{allLabel}</option>
      {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  );
}

export function DateInput({ value, onChange, label }) {
  return (
    <label className="date-filter">
      <span>{label}</span>
      <input type="date" className="input" value={value ?? ''} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}
