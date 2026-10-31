import { Outlet } from 'react-router-dom';

const LOGO = `${import.meta.env.BASE_URL}logo/`;

export default function AuthLayout() {
  return (
    <div className="auth">
      <section className="auth-art">
        <img className="auth-logo" src={`${LOGO}logo-dark.svg`} alt="Huilerie Ben Chaaben — Huile d'olive extra vierge, depuis 1948, Tunisie" />
        <p>Achats, pressage, stock et ventes de l'huilerie, au même endroit.</p>
      </section>
      <section className="auth-form">
        <div className="auth-card">
          {/* Sur mobile le panneau de gauche est masqué : on garde le logo au-dessus du formulaire. */}
          <img className="auth-logo-mobile" src={`${LOGO}logo-light.svg`} alt="Huilerie Ben Chaaben" />
          <Outlet />
        </div>
      </section>
    </div>
  );
}
