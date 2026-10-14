import { Lock } from 'lucide-react';
import { Link } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState';

export default function ForbiddenPage() {
  return (
    <div className="panel">
      <EmptyState icon={Lock} title="Accès non autorisé"
                  text="Votre rôle ne donne pas accès à cette page. Demandez à l'administrateur si vous en avez besoin."
                  action={<Link className="btn btn-secondary" to="/">Retour au tableau de bord</Link>} />
    </div>
  );
}
