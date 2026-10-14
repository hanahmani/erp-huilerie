import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { notificationService } from '../../services/salesDocService';

const LEVEL = { ALERTE: 'notif-alert', AVERTISSEMENT: 'notif-warn', INFO: 'notif-info' };

/** Cloche : alertes calculées par le serveur, rafraîchies toutes les 2 minutes. */
export default function NotificationBell() {
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  const load = useCallback(() => notificationService.list().then(setItems).catch(() => {}), []);
  useEffect(() => {
    load();
    const t = setInterval(load, 120000);
    return () => clearInterval(t);
  }, [load]);
  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const unread = items.filter((n) => !n.lu);
  const markAll = async () => {
    await notificationService.read(unread.map((n) => n.key)).catch(() => {});
    load();
  };
  const go = async (n) => {
    setOpen(false);
    if (!n.lu) await notificationService.read([n.key]).catch(() => {});
    load();
    navigate(n.lien);
  };

  return (
    <div className="user-menu" ref={ref}>
      <button type="button" className="btn btn-ghost btn-sm bell" onClick={() => setOpen((v) => !v)} aria-label={`Notifications (${unread.length} non lues)`}>
        <Bell size={20} />
        {unread.length ? <span className="bell-count">{unread.length > 99 ? '99+' : unread.length}</span> : null}
      </button>
      {open ? (
        <div className="dropdown notif-dropdown" role="menu">
          <div className="notif-head"><strong>Notifications</strong>
            {unread.length ? <button type="button" className="link-btn" onClick={markAll}>Tout marquer comme lu</button> : null}</div>
          {items.length ? items.slice(0, 8).map((n) => (
            <button key={n.key} type="button" className={`notif-item ${LEVEL[n.niveau]}${n.lu ? ' read' : ''}`} onClick={() => go(n)}>
              <strong>{n.titre}</strong><span>{n.message}</span>
            </button>
          )) : <p className="muted" style={{ padding: 10 }}>Aucune alerte en cours.</p>}
          <Link to="/notifications" className="notif-all" onClick={() => setOpen(false)}>Voir toutes les notifications</Link>
        </div>
      ) : null}
    </div>
  );
}
