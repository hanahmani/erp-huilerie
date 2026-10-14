import { useState } from 'react';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import { useAuth } from '../../context/AuthContext';
import { catalogService } from '../../services/catalogService';
import { referenceService } from '../../services/referenceService';
import CompanySettingsForm from './CompanySettingsForm';
import SimpleRefTable from './SimpleRefTable';

export default function SettingsPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState(can('SETTINGS_MANAGE') ? 'company' : 'units');
  const canCatalog = can('CATALOG_MANAGE');

  return (
    <>
      <PageHeader title="Paramètres" description="Informations légales de l'huilerie et référentiels utilisés partout dans l'ERP." />
      <Tabs value={tab} onChange={setTab}
            tabs={[{ value: 'company', label: "L'huilerie" }, { value: 'units', label: 'Unités' }, { value: 'categories', label: 'Catégories' },
                   { value: 'taxes', label: 'Taxes' }, { value: 'expenses', label: 'Dépenses' }, { value: 'other', label: 'Monnaie et paiements' }]} />
      {tab === 'company' ? <CompanySettingsForm /> : null}
      {tab === 'units' ? (
        <SimpleRefTable label="Unités de mesure" createLabel="Nouvelle unité" canManage={canCatalog}
                        emptyText="Créez les unités utilisées : L (litre), kg, bouteille, bidon, carton…"
                        load={catalogService.units} save={catalogService.saveUnit} setStatus={catalogService.unitStatus}
                        columns={[{ key: 'code', header: 'Code', render: (u) => <strong>{u.code}</strong> },
                                  { key: 'libelle', header: 'Libellé' }]}
                        fields={[{ name: 'code', label: 'Code', required: true, hint: 'Court, affiché sur les tickets. Ex. L' },
                                 { name: 'libelle', label: 'Libellé', required: true }]} />
      ) : null}
      {tab === 'categories' ? (
        <SimpleRefTable label="Catégories d'articles" createLabel="Nouvelle catégorie" canManage={canCatalog}
                        emptyText="Exemples : Huile extra vierge, Huile vierge, Emballages."
                        load={catalogService.categories} save={catalogService.saveCategory} setStatus={catalogService.categoryStatus}
                        columns={[{ key: 'nom', header: 'Nom', render: (c) => <strong>{c.nom}</strong> },
                                  { key: 'description', header: 'Description', render: (c) => c.description || '—' }]}
                        fields={[{ name: 'nom', label: 'Nom', required: true },
                                 { name: 'description', label: 'Description', wide: true }]} />
      ) : null}
      {tab === 'taxes' ? (
        <SimpleRefTable label="Taux de TVA" createLabel="Nouvelle taxe" canManage={can('SETTINGS_MANAGE')}
                        emptyText="Saisissez les taux applicables à vos achats, par exemple TVA 19 %, 13 %, 7 % ou exonéré 0 %."
                        load={referenceService.taxes} save={referenceService.saveTax} setStatus={referenceService.taxStatus}
                        columns={[{ key: 'nom', header: 'Nom', render: (t) => <strong>{t.nom}</strong> },
                                  { key: 'taux', header: 'Taux', render: (t) => `${Number(t.taux)} %` }]}
                        fields={[{ name: 'nom', label: 'Nom', required: true, hint: 'Ex. TVA 19 %' },
                                 { name: 'taux', label: 'Taux (%)', type: 'number', required: true }]} />
      ) : null}
      {tab === 'expenses' ? (
        <SimpleRefTable label="Catégories de dépenses" createLabel="Nouvelle catégorie" canManage={can(['SETTINGS_MANAGE', 'FINANCE_MANAGE'])}
                        emptyText="Exemples : électricité, eau, carburant, main-d'œuvre saisonnière, entretien des machines."
                        load={referenceService.expenseCategories} save={referenceService.saveExpenseCategory}
                        setStatus={referenceService.expenseCategoryStatus}
                        columns={[{ key: 'nom', header: 'Nom', render: (c) => <strong>{c.nom}</strong> }]}
                        fields={[{ name: 'nom', label: 'Nom', required: true }]} />
      ) : null}
      {tab === 'other' ? (
        <div className="panel" style={{ maxWidth: 760 }}>
          <div className="panel-body">
            <dl className="kv">
              <dt>Devise</dt><dd>Dinar tunisien (DT), 3 décimales (millimes). Tous les montants sont calculés par le serveur.</dd>
              <dt>Moyens de paiement</dt><dd>Espèces, carte, virement, chèque. Seules les espèces permettent de rendre la monnaie.</dd>
              <dt>Numérotation</dt><dd>Séquentielle et sans trou par type et par année : TCK (tickets), ACH (achats), REC (réceptions), PFR (paiements fournisseurs), RGL (règlements clients), DEP (dépenses), INV (inventaires).</dd>
              <dt>Crédit client</dt><dd>Activé ou non dans l'onglet « L'huilerie » ; plafond par client dans la fiche client.</dd>
              <dt>Valorisation du stock</dt><dd>Au dernier prix d'achat saisi sur l'article.</dd>
            </dl>
          </div>
        </div>
      ) : null}
    </>
  );
}
