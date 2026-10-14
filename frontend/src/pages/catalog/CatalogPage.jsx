import { useEffect, useState } from 'react';
import { BookOpen } from 'lucide-react';
import ManagedList from '../../components/common/ManagedList';
import Alert from '../../components/common/Alert';
import { catalogService } from '../../services/catalogService';
import { ARTICLE_TYPES, toOptions } from '../../utils/labels';
import { formatDT, formatQty } from '../../utils/format';

export default function CatalogPage() {
  const [units, setUnits] = useState([]);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([catalogService.units(), catalogService.categories()])
      .then(([u, c]) => { setUnits(u); setCategories(c); })
      .catch((e) => setError(e.message));
  }, []);

  const fields = (article) => [
    { name: 'reference', label: 'Référence', required: true, hint: 'Unique. Ex. HEV-1L' },
    { name: 'nom', label: 'Désignation', required: true },
    { name: 'type', label: 'Type', type: 'select', options: toOptions(ARTICLE_TYPES), required: true },
    {
      name: 'categorieId', label: 'Catégorie', type: 'select', numeric: true, placeholder: 'Aucune',
      options: categories.filter((c) => c.actif || c.id === article.categorieId).map((c) => ({ value: c.id, label: c.nom })),
    },
    {
      name: 'uniteId', label: 'Unité de vente et de stock', type: 'select', numeric: true, required: true,
      options: units.filter((u) => u.actif || u.id === article.uniteId).map((u) => ({ value: u.id, label: `${u.libelle} (${u.code})` })),
    },
    { name: 'contenance', label: 'Contenance en litres', type: 'number', hint: 'Ex. 5 pour un bidon de 5 L. Vide si non concerné.' },
    { name: 'prixAchat', label: "Prix d'achat (DT)", type: 'number' },
    { name: 'prixVente', label: 'Prix de vente (DT)', type: 'number', hint: 'Obligatoire si vendable.' },
    { name: 'seuilMin', label: 'Seuil d\'alerte de stock', type: 'number', hint: 'Dans l\'unité de l\'article.' },
    { name: 'stockMax', label: 'Stock maximum', type: 'number', hint: 'Facultatif.' },
    { name: 'codeBarres', label: 'Code-barres' },
    { name: 'description', label: 'Description', type: 'textarea', wide: true },
    { name: 'vendable', label: 'Vendable en caisse', type: 'checkbox' },
    {
      name: 'suiviLot', label: 'Suivi par lot', type: 'checkbox', disabled: Boolean(article.id),
      hint: article.id ? 'Non modifiable après création.' : 'Obligatoire pour l\'huile en vrac et l\'huile des clients.',
    },
  ];

  return (
    <>
      {error ? <Alert type="error">{error}</Alert> : null}
      {!units.length && !error ? (
        <Alert type="info">Créez d'abord les unités de mesure dans Paramètres, onglet Unités.</Alert>
      ) : null}
      <ManagedList
        title="Catalogue" icon={BookOpen} entityLabel="Article" createLabel="Nouvel article" managePermission="CATALOG_MANAGE"
        description="Huiles, olives, bouteilles, bidons, emballages et consommables."
        emptyText="Ajoutez votre premier article : huile extra vierge, bidon de 5 L, bouteille…"
        fetcher={catalogService.articles}
        filters={[
          { key: 'type', label: 'Type', allLabel: 'Tous les types', options: toOptions(ARTICLE_TYPES) },
          { key: 'categoryId', label: 'Catégorie', allLabel: 'Toutes les catégories', options: categories.map((c) => ({ value: c.id, label: c.nom })) },
        ]}
        columns={[
          { key: 'reference', header: 'Référence', render: (a) => <span className="muted">{a.reference}</span> },
          { key: 'nom', header: 'Désignation', render: (a) => <strong>{a.nom}</strong> },
          { key: 'type', header: 'Type', render: (a) => ARTICLE_TYPES[a.type] },
          { key: 'unite', header: 'Unité', render: (a) => a.uniteCode },
          { key: 'prixVente', header: 'Prix de vente', align: 'right', render: (a) => (a.prixVente != null ? formatDT(a.prixVente) : '—') },
          { key: 'seuil', header: 'Seuil', align: 'right', render: (a) => (a.seuilMin != null ? formatQty(a.seuilMin, a.uniteCode) : '—') },
          {
            key: 'flags', header: '',
            render: (a) => (
              <>
                {a.vendable ? <span className="badge badge-role">Vendable</span> : null}
                {a.suiviLot ? <span className="badge badge-role">Lots</span> : null}
              </>
            ),
          },
        ]}
        fields={fields}
        toForm={(a) => ({ ...a, categorieId: a.categorieId ?? '', uniteId: a.uniteId ?? '' })}
        onSave={catalogService.saveArticle}
        onStatus={catalogService.articleStatus}
      />
    </>
  );
}
