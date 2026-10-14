import { useId } from 'react';

/**
 * Rendu générique d'un formulaire à partir d'une configuration :
 * { name, label, type: text|number|select|checkbox|textarea|email, options, hint, required, step, wide, disabled }
 */
function Field({ f, value, error, onChange }) {
  const id = useId();
  if (f.type === 'checkbox') {
    return (
      <label className={`check check-field${f.wide ? ' wide' : ''}`}>
        <input type="checkbox" checked={Boolean(value)} disabled={f.disabled}
               onChange={(e) => onChange(f.name, e.target.checked)} />
        <span>
          {f.label}
          {f.hint ? <small>{f.hint}</small> : null}
        </span>
      </label>
    );
  }
  const common = {
    id, name: f.name, disabled: f.disabled, 'aria-invalid': Boolean(error),
    value: value ?? '', onChange: (e) => onChange(f.name, e.target.value),
  };
  let control;
  if (f.type === 'select') {
    control = (
      <select className="select" {...common}>
        {f.placeholder !== false ? <option value="">{f.placeholder || 'Choisir…'}</option> : null}
        {f.options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    );
  } else if (f.type === 'textarea') {
    control = <textarea className="input textarea" rows={3} {...common} />;
  } else {
    control = (
      <input className="input" type={f.type === 'number' ? 'text' : (f.type || 'text')}
             inputMode={f.type === 'number' ? 'decimal' : undefined} autoComplete="off" {...common} />
    );
  }
  return (
    <div className={`field${f.wide ? ' wide' : ''}`}>
      <label htmlFor={id}>{f.label}{f.required ? ' *' : ''}</label>
      {control}
      {error ? <span className="error">{error}</span> : f.hint ? <span className="hint">{f.hint}</span> : null}
    </div>
  );
}

export default function FormFields({ fields, values, errors = {}, onChange }) {
  return (
    <div className="form-grid">
      {fields.filter((f) => !f.hidden?.(values)).map((f) => (
        <Field key={f.name} f={f} value={values[f.name]} error={errors[f.name]} onChange={onChange} />
      ))}
    </div>
  );
}

/** Transforme les valeurs du formulaire en corps JSON : '' -> null, nombres en chaînes décimales. */
export function toPayload(fields, values) {
  const out = {};
  fields.forEach((f) => {
    const v = values[f.name];
    if (f.type === 'checkbox') out[f.name] = Boolean(v);
    else if (v === '' || v === undefined || v === null) out[f.name] = null;
    else if (f.type === 'number') out[f.name] = String(v).replace(',', '.').trim();
    else if (f.numeric) out[f.name] = Number(v);
    else out[f.name] = typeof v === 'string' ? v.trim() : v;
  });
  return out;
}
