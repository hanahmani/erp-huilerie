# Avancement — ERP Huilerie Ben Chaaben (v0.4)

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
| 12 | Ventes | 🟡 | Vente rapide, historique, annulation, crédit et règlements. ⬜ Devis, commandes, livraisons, retours. |
| 13 | Facturation | 🟡 | Tickets 80/58/A4. ⬜ Factures clients avec TVA, avoirs, PDF. |
| 14 | Paiements | ✅ | Paiements fournisseurs, règlements de ventes à crédit, partiels, reste dû. |
| — | Pressage client | ✅ | Réception et pesée (brut, tare, net, variété, état), tarif au kg, suivi du pressage, huile en lot CLIENT, encaissement, remises partielles, bons de pesée et de remise 80/58/A4. |
| 15 | Production (trituration) | ✅ | Planification, consommation des lots d'olives au démarrage, huile en lot, sous-produits, pertes, rendement, acidité et catégorie, lot bloqué en attente d'analyse. 🟡 Pas de bilan matière bloquant. |
| 16 | Matières premières | ✅ | Olives achetées (réception d'achat avec lot), stock par lot, consommation en production. 🟡 Pas d'alerte de délai d'attente avant trituration. |
| 17 | Lots et traçabilité | ✅ | Amont (achat, pressage, trituration, conditionnement) et aval (trituration, conditionnement, ventes, remises), navigation de lot en lot, historique des mouvements. |
| 18 | Contrôle qualité | 🟡 | Acidité et classement indicatif à la production, blocage / déblocage de lot. ⬜ Fiches de contrôle multi-critères, anomalies. |
| — | Conditionnement | ✅ | Vrac → bouteilles/bidons (contenance), emballages consommés, lot hérité, annulation par mouvements inverses. |
| 19–20 | RH, congés | ⬜ | |
| 21 | Finance | ✅ | Journal de caisse, sessions de caisse avec écarts, dépenses par catégorie, créances, dettes. |
| 22 | Rapports | ✅ | Ventes, achats, stock valorisé, trésorerie, dépenses, production et pressage (rendement par variété, par catégorie), conditionnement ; export CSV, impression. ⬜ Rapport utilisateurs. |
| 23 | Notifications | ⬜ | Les alertes sont visibles sur le tableau de bord, pas de centre de notifications. |
| 24 | Recherche globale | ⬜ | Recherche par module uniquement. |
| 25 | Paramétrage | ✅ | Huilerie, unités, catégories, taxes, catégories de dépenses, crédit. Devise fixe DT. |
| 26 | Journal d'audit | ✅ | Écriture partout + écran de consultation. ⬜ Ancienne/nouvelle valeur détaillées, adresse IP. |
| 27 | Sécurité | 🟡 | Voir README §5. ⬜ Limitation des tentatives, en-têtes de sécurité, sauvegarde planifiée. |
