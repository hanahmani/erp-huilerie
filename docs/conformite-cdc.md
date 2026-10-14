# Correspondance avec le cahier des charges (v0.5)

✅ couvert · 🟡 partiel · ⬜ non couvert. Le détail technique est dans `avancement.md`.

| § | Exigence | État | Où dans l'ERP |
|---|---|---|---|
| 2 | Acteurs : administrateur, responsable, employé | ✅ | Rôles système Administrateur, Responsables (stock, achats, production, commercial, qualité, financier, RH), Caissier, Employé ; rôles personnalisés |
| 3 | Authentification, sessions, comptes désactivés, changement / réinitialisation du mot de passe | ✅ | Connexion ; réinitialisation par l'administrateur (mot de passe provisoire). 🟡 pas de réinitialisation par e-mail |
| 4 | Tableau de bord : indicateurs, alertes, activité, graphiques | ✅ | Tableau de bord par rôle ; filtres par période et exports dans Rapports |
| 5 | Utilisateurs (nom, login, e-mail, téléphone, service, statut, création, dernière connexion) | ✅ | Administration › Utilisateurs |
| 6 | Rôles et permissions par module et par action | ✅ | Administration › Rôles et permissions (matrice) |
| 7 | Clients (code, coordonnées, e-mail, MF, conditions de paiement, plafond, historique, export) | ✅ | Clients ; historique via Ventes et facturation, Créances |
| 8 | Fournisseurs (e-mail, MF, conditions, banque et RIB, historique) | ✅ | Fournisseurs ; historique via Achats |
| 9 | Articles (catégories, unités, prix, stock min / max, statut) | ✅ | Catalogue |
| 10 | Stocks : entrées, sorties, inventaire, corrections, mouvements, alertes | ✅ | Stock. 🟡 **un seul dépôt** : pas de transfert ni de filtre par dépôt |
| 11 | Achats : commande, validation, réception partielle, facture et paiement | ✅ | Achats. 🟡 pas d'étape « demande d'achat » distincte ni de retour fournisseur |
| 12 | Ventes : devis → commande → livraison → facturation, retours | ✅ | Ventes et facturation ; retour par avoir avec réintégration ; caisse rapide |
| 13 | Facturation : brouillon, validation, annulation, TVA, remise, HT/TVA/TTC, impression, PDF, impayés | ✅ | Factures (timbre fiscal, montant en lettres, impression A4 → PDF), avoirs |
| 14 | Paiements : partiels, impayés, par client / fournisseur, période | ✅ | Factures, Achats, Finance › Créances / Dettes, Journal de caisse |
| 15 | Production : ordres, consommations, produits finis, sous-produits, pertes, lots | ✅ | Production (trituration), Conditionnement |
| 16 | Matières premières : réception, pesée, lot, qualité, consommation | ✅ | Achats (olives), Pressage client (pesée), Production |
| 17 | Lots et traçabilité amont / aval | ✅ | Stock › Lots › Traçabilité |
| 18 | Contrôle qualité : critères, résultats, validation / rejet, anomalies | ✅ | Contrôle qualité |
| 19 | RH : employés, postes, services, contrats, historique | ✅ | RH et congés |
| 20 | Congés et absences : demande, validation, refus, solde, notifications | ✅ | RH et congés ; alertes dans la cloche |
| 21 | Finance : recettes, dépenses, comptes, créances, dettes, trésorerie | ✅ | Finance et caisse (caisse = compte unique ; 🟡 pas de comptes bancaires séparés) |
| 22 | Rapports et exports Excel / PDF / impression | ✅ | Rapports (CSV pour Excel, impression → PDF) ; activités : journal |
| 23 | Notifications : stock, paiement, validation, production, historique, marquer comme lu | ✅ | Cloche et page Notifications. 🟡 pas d'envoi d'e-mail |
| 24 | Recherche globale | ✅ | Ctrl+K dans la barre supérieure |
| 25 | Paramétrage : entreprise, unités, catégories, taxes, règles | ✅ | Paramètres. Devise fixe DT ; statuts fixés par les règles métier |
| 26 | Journal : utilisateur, date, action, élément, IP | ✅ | Journal d'activité. 🟡 ancienne / nouvelle valeur détaillées non enregistrées |
| 27 | Sécurité : mots de passe hachés, sessions, déconnexion pour inactivité, sauvegarde | ✅ | BCrypt, JWT révocable, inactivité paramétrable, `scripts/sauvegarde.sh` |
| 28 | Ergonomie responsive, navigateurs modernes | ✅ | Interface React responsive |
| 29 | Architecture en couches, API sécurisées | ✅ | React / Spring Boot / MySQL |
| 31 | Critères d'acceptation | 🟡 | Tests écrits pour chaque règle critique ; **à exécuter** (`mvn test`) |
| 32 | Livrables | 🟡 | Documentation technique et d'installation, permissions, sauvegarde. ⬜ manuel utilisateur détaillé, export OpenAPI |
| 33 | Évolutions (mobile, SMS, WhatsApp, BI) | ⬜ | Préparé : alertes calculées réutilisables, API REST |
