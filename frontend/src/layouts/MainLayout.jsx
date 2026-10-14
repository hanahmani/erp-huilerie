import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';
import Topbar from '../components/layout/Topbar';

export default function MainLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  return (
    <div className="app-shell">
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
