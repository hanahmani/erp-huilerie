import { Truck } from 'lucide-react';
import ManagedList from '../../components/common/ManagedList';
import { supplierService } from '../../services/supplierService';
import { SUPPLIER_TYPES, toOptions } from '../../utils/labels';

const fields = [
  { name: 'type', label: 'Type', type: 'select', options: toOptions(SUPPLIER_TYPES), required: true },
  { name: 'nom', label: 'Nom ou raison sociale', required: true },
  { name: 'cin', label: 'CIN', hint: "Agriculteur : CIN ou matricule fiscal." },
  { name: 'matriculeFiscal', label: 'Matricule fiscal', hint: 'Obligatoire pour une entreprise.' },
  { name: 'telephone', label: 'Téléphone' },
  { name: 'adresse', label: 'Adresse', type: 'textarea', wide: true },
];

export default function SuppliersPage() {
  return (
    <ManagedList
      title="Fournisseurs" icon={Truck} entityLabel="Fournisseur" createLabel="Nouveau fournisseur"
      managePermission="SUPPLIER_MANAGE"
      description="Agriculteurs qui vendent leurs olives et entreprises qui fournissent huile, bouteilles et emballages."
      emptyText="Ajoutez le premier fournisseur pour pouvoir enregistrer des achats."
      fetcher={supplierService.list}
      filters={[{ key: 'type', label: 'Type', allLabel: 'Tous les types', options: toOptions(SUPPLIER_TYPES) }]}
      columns={[
        { key: 'code', header: 'Code', render: (s) => <span className="muted">{s.code}</span> },
        { key: 'nom', header: 'Nom', render: (s) => <strong>{s.nom}</strong> },
        { key: 'type', header: 'Type', render: (s) => SUPPLIER_TYPES[s.type] },
        { key: 'id-fiscal', header: 'CIN / matricule', render: (s) => s.matriculeFiscal || s.cin || '—' },
        { key: 'telephone', header: 'Téléphone', render: (s) => s.telephone || '—' },
      ]}
      fields={fields}
      toForm={(s) => s}
      onSave={supplierService.save}
      onStatus={supplierService.setStatus}
    />
  );
}
