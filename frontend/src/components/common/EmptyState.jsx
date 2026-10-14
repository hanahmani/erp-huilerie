import { Check, Sprout } from 'lucide-react';

export default function EmptyState({ icon: Icon = Sprout, title, text, planned = [], action }) {
  return (
    <div className="empty">
      <div className="empty-icon"><Icon size={26} /></div>
      <h2>{title}</h2>
      {text ? <p>{text}</p> : null}
      {action ? <div style={{ marginTop: 18 }}>{action}</div> : null}
      {planned.length > 0 ? (
        <ul className="planned">
          {planned.map((item) => (
            <li key={item}><Check size={16} />{item}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
