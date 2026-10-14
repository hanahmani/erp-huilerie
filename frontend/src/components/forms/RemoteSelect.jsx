import { useEffect, useId, useState } from 'react';

/**
 * Liste déroulante alimentée par l'API avec recherche (articles, clients…).
 * fetcher(q) renvoie une page { content } ; toOption(item) => { value, label }.
 */
export default function RemoteSelect({ label, value, onChange, fetcher, toOption, placeholder = 'Choisir…', required, hint }) {
  const id = useId();
  const [q, setQ] = useState('');
  const [options, setOptions] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    let alive = true;
    const t = setTimeout(() => {
      fetcher(q).then((page) => alive && setOptions((page.content ?? page).map((it) => ({ ...toOption(it), item: it }))))
        .catch(() => alive && setOptions([]));
    }, 200);
    return () => { alive = false; clearTimeout(t); };
  }, [q, fetcher, toOption]);

  const all = selected && !options.some((o) => String(o.value) === String(selected.value)) ? [selected, ...options] : options;

  return (
    <div className="field">
      <label htmlFor={id}>{label}{required ? ' *' : ''}</label>
      <input className="input" placeholder="Rechercher…" value={q} onChange={(e) => setQ(e.target.value)}
             style={{ marginBottom: 6 }} aria-label={`Rechercher ${label}`} />
      <select id={id} className="select" value={value ?? ''}
              onChange={(e) => {
                const opt = all.find((o) => String(o.value) === e.target.value) || null;
                setSelected(opt);
                onChange(opt ? opt.value : '', opt?.item ?? null);
              }}>
        <option value="">{placeholder}</option>
        {all.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      {hint ? <span className="hint">{hint}</span> : null}
    </div>
  );
}
