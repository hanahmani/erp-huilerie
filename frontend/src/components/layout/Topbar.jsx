import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { KeyRound, LogOut, Menu } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { formatToday, initials } from '../../utils/format';
import { roleLabel } from '../../utils/roles';
import NotificationBell from './NotificationBell';
import GlobalSearch from './GlobalSearch';

export default function Topbar({ onToggleMenu }) {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/connexion', { replace: true });
  };

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button type="button" className="btn btn-ghost btn-sm menu-toggle" onClick={onToggleMenu} aria-label="Ouvrir le menu">
          <Menu size={20} />
        </button>
        <span className="today">{formatToday()}</span>
      </div>

      <div className="topbar-right">
      <GlobalSearch />
      <NotificationBell />
      <div className="user-menu" ref={ref}>
        <button type="button" className="user-btn" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
          <span className="avatar">{initials(user)}</span>
          <span className="user-meta">
            <strong>{user.firstName} {user.lastName}</strong>
            <span>{user.roles.map(roleLabel).join(', ')}</span>
          </span>
        </button>
        {open ? (
          <div className="dropdown" role="menu">
            <Link to="/profil" onClick={() => setOpen(false)}><KeyRound size={16} />Changer le mot de passe</Link>
            <button type="button" onClick={handleLogout}><LogOut size={16} />Se déconnecter</button>
          </div>
        ) : null}
      </div>
      </div>
    </header>
  );
}
