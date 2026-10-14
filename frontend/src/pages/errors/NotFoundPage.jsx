import { Compass } from 'lucide-react';
import { Link } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState';

export default function NotFoundPage() {
  return (
    <div className="panel">
      <EmptyState icon={Compass} title="Page introuvable" text="L'adresse demandée n'existe pas."
                  action={<Link className="btn btn-secondary" to="/">Retour au tableau de bord</Link>} />
    </div>
  );
}
