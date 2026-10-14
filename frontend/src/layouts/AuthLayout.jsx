import { Outlet } from 'react-router-dom';
import OliveMark from '../components/common/OliveMark';

function Branch() {
  return (
    <svg className="branch" viewBox="0 0 400 300" aria-hidden="true">
      <path d="M20 280 C 120 230, 230 160, 390 30" stroke="#6f8139" strokeWidth="5" fill="none" strokeLinecap="round" />
      <g fill="#5b6b2e">
        <ellipse cx="90" cy="232" rx="42" ry="12" transform="rotate(-50 90 232)" />
        <ellipse cx="150" cy="222" rx="40" ry="11" transform="rotate(15 150 222)" />
        <ellipse cx="190" cy="160" rx="44" ry="12" transform="rotate(-55 190 160)" />
        <ellipse cx="255" cy="150" rx="40" ry="11" transform="rotate(10 255 150)" />
        <ellipse cx="290" cy="90" rx="42" ry="11" transform="rotate(-60 290 90)" />
        <ellipse cx="345" cy="78" rx="34" ry="10" transform="rotate(5 345 78)" />
      </g>
      <g fill="#b8892a">
        <ellipse cx="215" cy="198" rx="14" ry="18" />
        <ellipse cx="318" cy="122" rx="12" ry="16" />
      </g>
      <ellipse cx="124" cy="252" rx="13" ry="17" fill="#8e9c55" />
    </svg>
  );
}

export default function AuthLayout() {
  return (
    <div className="auth">
      <section className="auth-art">
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, position: 'relative' }}>
          <OliveMark size={52} />
          <div className="brand-name">Huilerie<br />Ben Chaaben</div>
        </div>
        <p>Achats, pressage, stock et ventes de l'huilerie, au même endroit.</p>
        <Branch />
      </section>
      <section className="auth-form">
        <div className="auth-card">
          <Outlet />
        </div>
      </section>
    </div>
  );
}
