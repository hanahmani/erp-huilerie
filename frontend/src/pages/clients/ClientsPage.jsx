import { Users } from 'lucide-react';
import ManagedList from '../../components/common/ManagedList';
import { clientService } from '../../services/clientService';
import { CLIENT_TYPES, toOptions } from '../../utils/labels';
import { formatDT } from '../../utils/format';

const fields = [
  { name: 'type', label: 'Type', type: 'select', options: toOptions(CLIENT_TYPES), required: true },
  { name: 'nom', label: 'Nom ou raison sociale', required: true },
  { name: 'telephone', label: 'Téléphone' },
  { name: 'cin', label: 'CIN', hidden: (v) => v.type === 'PROFESSIONNEL' },
  { name: 'matriculeFiscal', label: 'Matricule fiscal', required: false, hint: 'Obligatoire pour un professionnel.' },
  { name: 'plafondCredit', label: 'Plafond de crédit (DT)', type: 'number', hint: 'Vide = pas de plafond.' },
  { name: 'adresse', label: 'Adresse', type: 'textarea', wide: true },
];

export default function ClientsPage() {
  return (
    <ManagedList
      title="Clients" icon={Users} entityLabel="Client" createLabel="Nouveau client" managePermission="CLIENT_MANAGE"
      description="Clients de l'huilerie : particuliers, agriculteurs et professionnels, avec leur solde dû."
      emptyText="Ajoutez le premier client. Les ventes au comptoir n'en ont pas besoin."
      fetcher={clientService.list}
      filters={[{ key: 'type', label: 'Type', allLabel: 'Tous les types', options: toOptions(CLIENT_TYPES) }]}
      columns={[
        { key: 'code', header: 'Code', render: (c) => <span className="muted">{c.code}</span> },
        { key: 'nom', header: 'Nom', render: (c) => <strong>{c.nom}</strong> },
        { key: 'type', header: 'Type', render: (c) => CLIENT_TYPES[c.type] },
        { key: 'telephone', header: 'Téléphone', render: (c) => c.telephone || '—' },
        { key: 'plafond', header: 'Plafond', align: 'right', render: (c) => (c.plafondCredit != null ? formatDT(c.plafondCredit) : '—') },
        {
          key: 'solde', header: 'Reste dû', align: 'right',
          render: (c) => (Number(c.solde) > 0 ? <strong>{formatDT(c.solde)}</strong> : <span className="muted">{formatDT(0)}</span>),
        },
      ]}
      fields={fields}
      toForm={(c) => ({ ...c, plafondCredit: c.plafondCredit ?? '' })}
      onSave={clientService.save}
      onStatus={clientService.setStatus}
    />
  );
}
