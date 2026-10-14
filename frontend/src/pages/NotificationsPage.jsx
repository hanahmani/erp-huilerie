import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import PageHeader from '../components/common/PageHeader';
import Button from '../components/common/Button';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { notificationService } from '../services/salesDocService';

const TYPES = { STOCK: 'Stock', PAIEMENT: 'Paiements', VALIDATION: 'Validations', PRODUCTION: 'Production', RH: 'RH', QUALITE: 'Qualité' };

export default function NotificationsPage() {
  const [items, setItems] = useState(null);
  const navigate = useNavigate();
  const load = () => notificationService.list().then(setItems).catch(() => setItems([]));
  useEffect(() => { load(); }, []);
  if (!items) return <Spinner />;
  const unread = items.filter((n) => !n.lu);
  return (
    <>
      <PageHeader title="Notifications" description="Alertes calculées à partir des données réelles : elles disparaissent quand leur cause est réglée."
                  actions={unread.length ? <Button variant="secondary" onClick={async () => { await notificationService.read(unread.map((n) => n.key)); load(); }}>Tout marquer comme lu</Button> : null} />
      <div className="panel">
        {items.length ? items.map((n) => (
          <button key={n.key} type="button" className={`notif-row notif-${n.niveau === 'ALERTE' ? 'alert' : n.niveau === 'AVERTISSEMENT' ? 'warn' : 'info'}${n.lu ? ' read' : ''}`}
                  onClick={async () => { if (!n.lu) await notificationService.read([n.key]); navigate(n.lien); }}>
            <span className="badge">{TYPES[n.type] || n.type}</span>
            <strong>{n.titre}</strong>
            <span>{n.message}</span>
            {!n.lu ? <span className="badge badge-role">Nouveau</span> : null}
          </button>
        )) : <EmptyState icon={Bell} title="Aucune alerte" text="Tout est à jour." />}
      </div>
    </>
  );
}
