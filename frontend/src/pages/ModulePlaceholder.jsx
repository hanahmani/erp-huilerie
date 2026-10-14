import { Hourglass } from 'lucide-react';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import { MODULES } from '../utils/navigation';

/** Page d'un module pas encore développé : aucune donnée inventée, seulement ce qui est prévu. */
export default function ModulePlaceholder({ moduleKey }) {
  const m = MODULES[moduleKey];
  return (
    <>
      <PageHeader title={m.title} description={m.description} />
      <div className="panel">
        <EmptyState
          icon={Hourglass}
          title={`Ce module arrive à la phase ${m.phase}`}
          text="Les écrans seront ajoutés ici, branchés sur les données réelles de la base."
          planned={m.planned}
        />
      </div>
    </>
  );
}
