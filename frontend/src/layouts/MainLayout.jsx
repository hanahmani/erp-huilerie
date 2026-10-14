import { useCallback, useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useInactivityLogout } from '../hooks/useInactivityLogout';
import { settingsService } from '../services/settingsService';
import Sidebar from '../components/layout/Sidebar';
import Topbar from '../components/layout/Topbar';

export default function MainLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [timeout, setTimeoutMin] = useState(30);
  const { logout } = useAuth();
  const navigate = useNavigate();
  useEffect(() => { settingsService.get().then((s) => s.inactiviteMinutes && setTimeoutMin(s.inactiviteMinutes)).catch(() => {}); }, []);
  const onIdle = useCallback(() => { logout(); navigate('/connexion', { replace: true, state: { idle: true } }); }, [logout, navigate]);
  const warning = useInactivityLogout(timeout, onIdle);
  return (
    <div className="app-shell">
      {warning ? <div className="idle-warning" role="alert">Inactivité : déconnexion automatique dans moins d'une minute.</div> : null}
      <Sidebar open={menuOpen} onNavigate={() => setMenuOpen(false)} />
      <div className="main" onClick={() => menuOpen && setMenuOpen(false)}>
        <Topbar onToggleMenu={(e) => { e.stopPropagation(); setMenuOpen((v) => !v); }} />
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
