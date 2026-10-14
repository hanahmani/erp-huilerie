# Avancement — ERP Huilerie Ben Chaaben (v0.5)

Légende : ✅ fait (BDD + API + écrans) · 🟡 partiel · ⬜ à faire.
Le README précise ce qui a réellement été compilé et exécuté.

| § CDC | Module | État | Remarques |
|---|---|---|---|
| 3 | Authentification | ✅ | Connexion, sessions JWT, changement et réinitialisation du mot de passe par l'admin. ⬜ Réinitialisation par e-mail, verrouillage après N échecs, déconnexion pour inactivité. |
| 4 | Tableau de bord | ✅ | KPI ventes, caisse, achats, dettes, dépenses, stock faible, lots bloqués, graphique 7 jours, filtré par rôle. |
| 5 | Utilisateurs | ✅ | Liste, recherche, filtres, création, modification, désactivation, réactivation, suppression (compte jamais utilisé), mot de passe provisoire, e-mail, téléphone, service, dernière connexion. |
| 6 | Rôles et permissions | ✅ | Rôles système et personnalisés, matrice par module, effet immédiat. |
| 7 | Clients | ✅ | Fiche, recherche, filtres, reste dû, plafond de crédit. 🟡 Pas d'onglet historique dans la fiche (historique via ventes et créances). |
| 8 | Fournisseurs | ✅ | Fiche, recherche, filtres. ⬜ Informations bancaires. |
| 9 | Articles | ✅ | Catégories, unités, prix, seuil, code-barres, suivi par lot. ⬜ Stock maximum. |
| 10 | Stocks | ✅ | État, mouvements, lots, blocage, inventaires physiques, export. ⬜ Multi-dépôts et transferts (un seul dépôt). |
| 11 | Achats | ✅ | Commande → validation → réceptions partielles → paiements, TVA, remise, annulation contrôlée, réf. facture fournisseur, export. ⬜ Demande d'achat, retour fournisseur. |
| 12 | Ventes | ✅ | Vente rapide (caisse) ; devis → commande → livraison → facture, transformations, retours par avoir. |
| 13 | Facturation | ✅ | Factures clients (brouillon, validation numérotée, TVA, remise, timbre, échéance, paiements partiels, impayés), avoirs, impression A4 → PDF, montant en lettres. |
| 14 | Paiements | ✅ | Paiements fournisseurs, règlements de ventes à crédit, partiels, reste dû. |
| — | Pressage client | ✅ | Réception et pesée (brut, tare, net, variété, état), tarif au kg, suivi du pressage, huile en lot CLIENT, encaissement, remises partielles, bons de pesée et de remise 80/58/A4. |
| 15 | Production (trituration) | ✅ | Planification, consommation des lots d'olives au démarrage, huile en lot, sous-produits, pertes, rendement, acidité et catégorie, lot bloqué en attente d'analyse. 🟡 Pas de bilan matière bloquant. |
| 16 | Matières premières | ✅ | Olives achetées (réception d'achat avec lot), stock par lot, consommation en production. 🟡 Pas d'alerte de délai d'attente avant trituration. |
| 17 | Lots et traçabilité | ✅ | Amont (achat, pressage, trituration, conditionnement) et aval (trituration, conditionnement, ventes, remises), navigation de lot en lot, historique des mouvements. |
| 18 | Contrôle qualité | ✅ | Critères paramétrables, contrôles multi-critères, décision automatique (blocage / déblocage), anomalies et actions correctives. |
| — | Conditionnement | ✅ | Vrac → bouteilles/bidons (contenance), emballages consommés, lot hérité, annulation par mouvements inverses. |
| 19–20 | RH, congés | ✅ | Employés, services, postes, contrats et fins de contrat, lien compte utilisateur, demandes de congé (jours ouvrés, jours fériés, chevauchement, solde), validation / refus, absences. |
| 21 | Finance | ✅ | Journal de caisse, sessions de caisse avec écarts, dépenses par catégorie, créances, dettes. |
| 22 | Rapports | ✅ | Ventes, achats, stock valorisé, trésorerie, dépenses, production et pressage (rendement par variété, par catégorie), conditionnement ; export CSV, impression. ⬜ Rapport utilisateurs. |
| 23 | Notifications | ✅ | Cloche et page dédiée, alertes par permission, marquer comme lu. ⬜ e-mail. |
| 24 | Recherche globale | ✅ | Ctrl+K : clients, fournisseurs, articles, lots, tickets, documents, achats, pressages, production, employés. |
| 25 | Paramétrage | ✅ | Huilerie, unités, catégories, taxes, catégories de dépenses, crédit. Devise fixe DT. |
| 26 | Journal d'audit | ✅ | Écriture partout, adresse IP, écran de consultation. ⬜ Ancienne/nouvelle valeur détaillées. |
| 27 | Sécurité | ✅ | Déconnexion pour inactivité paramétrable, scripts de sauvegarde / restauration. ⬜ Limitation des tentatives de connexion. |
