import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { NAVIGATION } from '../../utils/navigation';
import OliveMark from '../common/OliveMark';

export default function Sidebar({ open, onNavigate }) {
  const { can } = useAuth();

  return (
    <aside className={`sidebar${open ? ' open' : ''}`}>
      <NavLink to="/" className="brand" onClick={onNavigate}>
        <OliveMark size={40} />
        <div>
          <div className="brand-name">Ben Chaaben</div>
          <div className="brand-sub">Gestion de l'huilerie</div>
        </div>
      </NavLink>

      <nav className="nav" aria-label="Navigation principale">
        {NAVIGATION.map((group) => {
          const items = group.items.filter((i) => can(i.permission));
          if (!items.length) return null;
          return (
            <div className="nav-group" key={group.title}>
              <div className="nav-group-title">{group.title}</div>
              {items.map((item) => (
                <div key={item.key}>
                  <NavLink to={item.children ? item.children[0].path : item.path} end={item.path === '/'}
                           className={({ isActive }) => `nav-link${isActive && !item.children ? ' active' : ''}`}
                           onClick={onNavigate}>
                    <item.icon size={18} />
                    {item.label}
                  </NavLink>
                  {item.children ? (
                    <div className="nav-sub">
                      {item.children.filter((c) => can(c.permission)).map((child) => (
                        <NavLink key={child.key} to={child.path} className="nav-link" onClick={onNavigate}>
                          {child.label}
                        </NavLink>
                      ))}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          );
        })}
      </nav>

      <div className="sidebar-foot">Version 0.4</div>
    </aside>
  );
}
