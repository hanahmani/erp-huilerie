import {
  LayoutDashboard, ShoppingBasket, Factory, Droplets, Package, Boxes, ReceiptText,
  Wallet, Users, Truck, BookOpen, ShieldCheck, Settings, BarChart3, KeyRound, History, FileText, ClipboardCheck, UserRound,
} from 'lucide-react';

/**
 * Menu de l'ERP. "permission" ne sert qu'à masquer un lien :
 * la vraie protection est faite par le backend (@PreAuthorize).
 * "planned" décrit les fonctionnalités prévues (texte d'aide, pas des données).
 */
export const NAVIGATION = [
  {
    title: 'Pilotage',
    items: [
      { key: 'dashboard', label: 'Tableau de bord', path: '/', icon: LayoutDashboard, permission: 'DASHBOARD_VIEW' },
    ],
  },
  {
    title: 'Activité',
    items: [
      {
        key: 'sales', label: 'Caisse', icon: ReceiptText, permission: 'SALES_VIEW', path: '/ventes',
        children: [
          { key: 'sales-quick', label: 'Vente rapide', path: '/ventes/rapide', permission: 'SALES_CREATE' },
          { key: 'sales-history', label: 'Tickets', path: '/ventes/historique', permission: 'SALES_VIEW' },
        ],
      },
      {
        key: 'salesdocs', label: 'Ventes et facturation', icon: FileText, permission: 'SALES_VIEW', path: '/ventes/devis',
        children: [
          { key: 'sd-devis', label: 'Devis', path: '/ventes/devis', permission: 'SALES_VIEW' },
          { key: 'sd-cmd', label: 'Commandes', path: '/ventes/commandes', permission: 'SALES_VIEW' },
          { key: 'sd-bl', label: 'Livraisons', path: '/ventes/livraisons', permission: 'SALES_VIEW' },
          { key: 'sd-fac', label: 'Factures', path: '/ventes/factures', permission: 'SALES_VIEW' },
          { key: 'sd-avr', label: 'Avoirs', path: '/ventes/avoirs', permission: 'SALES_VIEW' },
        ],
      },
      { key: 'pressing', label: 'Pressage client', path: '/pressage', icon: Droplets, permission: 'PRESSING_VIEW' },
      { key: 'production', label: 'Production', path: '/production', icon: Factory, permission: 'PRODUCTION_VIEW' },
      { key: 'packaging', label: 'Conditionnement', path: '/conditionnement', icon: Package, permission: 'PACKAGING_VIEW' },
      { key: 'quality', label: 'Contrôle qualité', path: '/qualite', icon: ClipboardCheck, permission: 'QUALITY_VIEW' },
      { key: 'purchases', label: 'Achats', path: '/achats', icon: ShoppingBasket, permission: 'PURCHASE_VIEW' },
    ],
  },
  {
    title: 'Stock et référentiels',
    items: [
      { key: 'stock', label: 'Stock', path: '/stock', icon: Boxes, permission: 'STOCK_VIEW' },
      { key: 'catalog', label: 'Catalogue', path: '/catalogue', icon: BookOpen, permission: 'CATALOG_VIEW' },
      { key: 'clients', label: 'Clients', path: '/clients', icon: Users, permission: 'CLIENT_VIEW' },
      { key: 'suppliers', label: 'Fournisseurs', path: '/fournisseurs', icon: Truck, permission: 'SUPPLIER_VIEW' },
    ],
  },
  {
    title: 'Gestion',
    items: [
      { key: 'finance', label: 'Finance et caisse', path: '/finance', icon: Wallet, permission: ['FINANCE_VIEW', 'CASH_SESSION'] },
      { key: 'reports', label: 'Rapports', path: '/rapports', icon: BarChart3, permission: 'REPORT_VIEW' },
      { key: 'hr', label: 'RH et congés', path: '/rh', icon: UserRound, permission: ['HR_VIEW', 'LEAVE_REQUEST', 'LEAVE_VALIDATE'] },
    ],
  },
  {
    title: 'Administration',
    items: [
      { key: 'users', label: 'Utilisateurs', path: '/utilisateurs', icon: ShieldCheck, permission: 'USER_MANAGE' },
      { key: 'roles', label: 'Rôles et permissions', path: '/roles', icon: KeyRound, permission: ['ROLE_MANAGE', 'USER_MANAGE'] },
      { key: 'audit', label: "Journal d'activité", path: '/journal', icon: History, permission: 'AUDIT_VIEW' },
      { key: 'settings', label: 'Paramètres', path: '/parametres', icon: Settings, permission: ['SETTINGS_MANAGE', 'CATALOG_MANAGE'] },
    ],
  },
];

/** Description des modules encore vides, affichée sur leur page. */
export const MODULES = {
  dashboard: { title: 'Tableau de bord', phase: 6, planned: ['Ventes du jour', 'Huile disponible', 'Achats du jour', 'Pressages en cours', 'Paiements en attente'] },
  clients: { title: 'Clients', phase: 2, description: 'Coordonnées, historique des opérations et soldes des clients.', planned: ['Liste avec recherche', 'Ajout et modification', 'Historique des ventes et pressages'] },
  suppliers: { title: 'Fournisseurs', phase: 2, description: 'Agriculteurs et fournisseurs d\'huile, avec leurs règlements.', planned: ['Liste avec recherche', 'Ajout et modification', 'Historique des achats et dettes'] },
  catalog: { title: 'Catalogue', phase: 2, description: 'Articles vendus ou achetés : olives, huile en vrac, bouteilles, bidons, consommables.', planned: ['Articles et références', 'Catégories', 'Unités : kg, litre, unité, bouteille, bidon', 'Formats de conditionnement'] },
  stock: { title: 'Stock', phase: 2, description: 'Quantités disponibles calculées à partir des mouvements enregistrés.', planned: ['Stock par article et par lot', 'Mouvements d\'entrée et de sortie', 'Inventaires', 'Alertes de stock faible'] },
  purchases: { title: 'Achats', phase: 3, description: 'Achats d\'olives (kg) et d\'huile (L) auprès des fournisseurs.', planned: ['Nouvel achat', 'Réception des marchandises', 'Factures et règlements'] },
  production: { title: 'Production', phase: 3, description: 'Transformation des olives de l\'huilerie en huile de l\'entreprise.', planned: ['Lots d\'olives', 'Fabrication', 'Rendement et pertes'] },
  pressing: { title: 'Pressage client', phase: 4, description: 'Olives apportées par les clients. Leur huile reste leur propriété et n\'entre jamais dans le stock de vente.', planned: ['Réception et pesée', 'Suivi du pressage', 'Remise de l\'huile', 'Tickets de réception et de remise'] },
  packaging: { title: 'Conditionnement', phase: 5, description: 'Mise en bouteilles et en bidons à partir de l\'huile en vrac.', planned: ['Ordres de conditionnement', 'Consommables utilisés', 'Produits finis en stock'] },
  salesQuick: { title: 'Vente rapide', phase: 5, description: 'Encaisser un client en quelques secondes et imprimer son ticket.', planned: ['Panier avec calcul automatique', 'Espèces, carte, virement', 'Contrôle du stock avant validation', 'Ticket 80 mm, 58 mm ou A4'] },
  salesHistory: { title: 'Historique des ventes', phase: 5, description: 'Toutes les ventes validées, avec réimpression du ticket.', planned: ['Filtres par date, ticket, client, caissier', 'Consultation et réimpression', 'Annulation autorisée et tracée'] },
  finance: { title: 'Finance', phase: 6, description: 'Caisse, dépenses, créances et dettes.', planned: ['Caisse et résumé journalier', 'Paiements et paiements partiels', 'Dépenses', 'Dettes fournisseurs et créances clients'] },
  users: { title: 'Utilisateurs', description: 'Comptes des employés, rôles et accès.' },
  quality: { title: 'Contrôle qualité', description: 'Analyses des lots, conformité et anomalies.' },
  hr: { title: 'RH et congés', description: 'Employés, contrats, congés et absences.' },
  salesdocs: { title: 'Ventes et facturation', description: 'Devis, commandes, livraisons, factures et avoirs.' },
  reports: { title: 'Rapports', description: 'Ventes, achats, stock valorisé et trésorerie, exportables.' },
  roles: { title: 'Rôles', description: 'Qui peut faire quoi, module par module.' },
  audit: { title: 'Journal', description: 'Historique des opérations sensibles.' },
  settings: { title: 'Paramètres', phase: 2, description: 'Informations légales de l\'huilerie, tarifs et préférences.', planned: ['Adresse, téléphone, matricule fiscal', 'Logo pour les tickets', 'Tarif du pressage', 'Unités et préférences'] },
};
