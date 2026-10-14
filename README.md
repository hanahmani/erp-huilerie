# ERP Huilerie Ben Chaaben

Application web de gestion de l'huilerie. Version **0.5** — correspondance avec le cahier des charges : `docs/conformite-cdc.md`.

- fondations : authentification, utilisateurs, rôles et permissions vérifiés côté serveur ;
- paramètres de l'huilerie, unités, catégories, articles, clients, fournisseurs ;
- stock calculé par mouvements, lots (huilerie / client), blocage de lot, stock négatif impossible ;
- vente rapide en caisse, tickets 80 mm / 58 mm / A4, historique, réimpression et annulation tracées ;
- journal de caisse et tableau de bord calculé ;
- achats : commandes, validation, réceptions partielles, paiements fournisseurs, TVA et remises ;
- stock : inventaires physiques avec génération des écarts ;
- finance : ouverture/fermeture de caisse avec écarts, dépenses, créances clients et règlements, dettes fournisseurs ;
- rapports : ventes, achats, stock valorisé, trésorerie, export CSV et impression ;
- administration : utilisateurs complets, rôles personnalisés avec matrice de permissions, journal d'activité, taxes ;
- pressage client : pesée, suivi, huile du client stockée à part, encaissement, remise, bons imprimables ;
- production : trituration des olives de l'huilerie, rendement, acidité et catégorie, sous-produits ;
- conditionnement : vrac → bouteilles et bidons, emballages consommés ;
- traçabilité des lots en amont et en aval ;
- ventes et facturation : devis, commandes, livraisons, factures (TVA, timbre, échéance, paiements partiels), avoirs ;
- contrôle qualité : critères, contrôles, blocage des lots non conformes, anomalies ;
- RH : employés, contrats, services, postes, congés (jours ouvrés, solde, validation), absences, jours fériés ;
- notifications (cloche), recherche globale (Ctrl+K), déconnexion après inactivité, scripts de sauvegarde.

État détaillé par module : `docs/avancement.md`. Choix techniques : `docs/decisions.md`.

| Partie       | Technologie                                             |
|--------------|---------------------------------------------------------|
| Frontend     | React 18 + Vite 5, React Router, icônes lucide-react    |
| Backend      | Spring Boot 3.5 + Java 21 + Maven                       |
| Base         | MySQL 8.4, migrations Flyway, Spring Data JPA           |
| Sécurité     | Spring Security, jeton JWT signé (HS256), BCrypt         |
| Déploiement  | Docker Compose : MySQL + backend + Nginx (frontend)     |
| Devise       | Dinar tunisien (DT), 3 décimales                        |

---

## 1. Lancement avec Docker (recommandé)

Prérequis : Docker Desktop (ou Docker Engine + plugin Compose).

```bash
cp .env.example .env
# Ouvrir .env et remplacer chaque CHANGE_ME :
#   MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD  -> mots de passe forts
#   JWT_SECRET                           -> openssl rand -base64 48
#   ADMIN_INITIAL_USERNAME               -> ex. admin
#   ADMIN_INITIAL_PASSWORD               -> 10 caractères min., lettres + chiffres

docker compose up -d --build
docker compose logs -f backend      # attendre « Started HuilerieErpApplication »
```

Ouvrir **http://localhost:8080** et se connecter avec l'administrateur initial.
L'ERP demande immédiatement de changer ce mot de passe provisoire.

Ensuite, **vider `ADMIN_INITIAL_PASSWORD` dans `.env`** puis `docker compose up -d` :
l'administrateur n'est créé qu'une seule fois, quand la table des utilisateurs est vide.

| Adresse                          | Rôle                                        |
|----------------------------------|---------------------------------------------|
| http://localhost:8080            | ERP (Nginx sert React et relaie `/api`)     |
| http://127.0.0.1:8090/api        | API directe (tests manuels)                 |
| 127.0.0.1:3307                   | MySQL pour MySQL Workbench                  |
| http://127.0.0.1:8090/actuator/health | État du backend et de la base          |

Commandes utiles :

```bash
docker compose ps                    # état des services
docker compose down                  # arrêter (les données restent)
docker compose down -v               # arrêter ET effacer la base (irréversible)
docker compose exec mysql mysql -u huilerie_app -p huilerie_erp
```

## 2. Lancement sans Docker (développement)

Prérequis : Java 21, Maven 3.9+, Node.js 20+, MySQL 8.4.

**Base de données** (MySQL Workbench) :

```sql
CREATE DATABASE huilerie_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'huilerie_app'@'localhost' IDENTIFIED BY 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON huilerie_erp.* TO 'huilerie_app'@'localhost';
```

**Backend** (port 8080) :

```bash
cd backend
export DB_PASSWORD='votre_mot_de_passe'
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_INITIAL_USERNAME=admin
export ADMIN_INITIAL_PASSWORD='ChoisirUnMotDePasse2026'
mvn spring-boot:run
```

Sous Windows PowerShell : `$env:DB_PASSWORD="..."` à la place de `export`.

**Frontend** (port 5173, `/api` redirigé vers le backend par Vite) :

```bash
cd frontend
npm install
npm run dev
```

## 3. Tests

```bash
cd backend && mvn test      # base H2 en mémoire, aucune installation MySQL nécessaire
cd frontend && npm run build
```

Tests backend fournis :

- `AuthControllerTest` : connexion valide, mot de passe faux (401 + trace d'audit), mots de passe
  hachés BCrypt, compte désactivé, `/me` sans jeton, jeton falsifié, changement de mot de passe qui
  invalide l'ancien jeton, mot de passe trop faible, absence de route d'inscription.
- `PermissionTest` : caissier refusé sur `/api/users` (403), administrateur autorisé,
  gestionnaire non-admin incapable de créer un administrateur, mot de passe provisoire qui bloque
  les modules, désactivation qui coupe la session immédiatement, validation des champs (400).
- `MigrationsTest` : migrations appliquées, rôles techniques présents, ADMIN possède toutes les permissions.
- `SaleCalculatorTest` : totaux, arrondi au millime, remise, monnaie, paiement mixte, carte plafonnée au total.
- `SaleServiceTest` : vente validée (stock, caisse, numéro), numéros séquentiels, stock insuffisant sans
  enregistrement partiel ni numéro consommé, double envoi, crédit refusé, remise et prix sans permission,
  huile d'un client invendable, réimpression sans effet sur le stock, annulation compensée.
- `StockServiceTest` : stock initial unique, stock négatif impossible, types automatiques interdits à la main,
  3 décimales maximum, permission d'ajustement.
- `PurchaseServiceTest` : totaux avec remise, aucun mouvement de stock avant réception, réceptions partielles
  puis complètes, sur-réception refusée, brouillon non réceptionnable, commande validée non modifiable,
  paiement plafonné au reste dû, annulation impossible après réception, validation sans permission refusée.
- `InventoryServiceTest` : validation qui ramène le stock au comptage, double validation refusée, validation sans comptage refusée.
- `TreasuryServiceTest` : dépense puis contre-passation, une seule caisse ouverte, écart de caisse à justifier.
- `PressingServiceTest` : pesée et montant, huile en lot client non vendable, remise bloquée tant que le service
  n'est pas payé, remise partielle puis soldée, sur-remise refusée, tare invalide, annulation après fin refusée.
- `ProductionServiceTest` : consommation des olives au démarrage, rendement, catégorie, sous-produits,
  olives insuffisantes, olives d'un client refusées, classement par acidité.
- `SalesDocumentServiceTest` : cycle devis → commande → livraison → facture, stock sorti une seule fois,
  paiements plafonnés, facture directe et avoir avec retour en stock, document validé non modifiable,
  plafond de crédit, validation de facture réservée.
- `HrServiceTest` : jours ouvrés et fériés, solde débité à la validation, chevauchement et solde insuffisant,
  refus motivé, contrat saisonnier sans date de fin.
- `QualityServiceTest` : non-conformité qui bloque le lot et ouvre une anomalie, conformité qui débloque.
- `PackagingServiceTest` : volume de vrac, emballages, produits finis, annulation compensée, vrac insuffisant,
  huile d'un client refusée, contenance obligatoire.

## 4. Variables d'environnement

| Variable                  | Obligatoire | Description                                              |
|---------------------------|-------------|----------------------------------------------------------|
| `MYSQL_PASSWORD`          | oui (Docker)| Mot de passe de l'utilisateur applicatif MySQL           |
| `MYSQL_ROOT_PASSWORD`     | oui (Docker)| Mot de passe root MySQL                                  |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | oui (hors Docker) | Connexion JDBC du backend         |
| `JWT_SECRET`              | oui         | Clé de signature des jetons, 32 caractères minimum       |
| `JWT_EXPIRATION_MINUTES`  | non (480)   | Durée de validité d'une session                          |
| `ADMIN_INITIAL_USERNAME`  | 1er démarrage | Identifiant du premier administrateur                  |
| `ADMIN_INITIAL_PASSWORD`  | 1er démarrage | Mot de passe provisoire, à retirer ensuite             |
| `CORS_ALLOWED_ORIGINS`    | non         | Origines autorisées si le frontend est sur un autre domaine |
| `APP_PORT`, `BACKEND_PORT`, `MYSQL_PORT` | non | Ports exposés par Docker                       |

Les fichiers `.env` ne sont jamais versionnés (voir `.gitignore`).

## 5. Règles de sécurité appliquées

- Mots de passe hachés avec BCrypt, jamais stockés ni journalisés en clair.
- Aucune inscription publique ; seuls les utilisateurs ayant `USER_MANAGE` créent des comptes,
  et seul un administrateur peut attribuer le rôle `ADMIN`.
- Permissions vérifiées côté backend (`@PreAuthorize`), le masquage des menus n'est qu'un confort.
- Le jeton ne contient pas les permissions : elles sont relues en base à chaque requête, donc
  retirer un rôle ou désactiver un compte prend effet immédiatement.
- Changer son mot de passe invalide toutes les sessions précédentes.
- Hibernate en mode `validate` : seul Flyway modifie le schéma.
- Base vide au départ : seuls les rôles et permissions techniques sont insérés (migration V2).

## 6. Premiers pas dans l'application

1. **Paramètres › L'huilerie** : raison sociale, adresse, téléphone, matricule fiscal (imprimés sur les tickets).
2. **Paramètres › Unités** puis **Catégories**.
3. **Catalogue** : créer les articles ; cocher « Vendable en caisse » avec un prix pour ceux vendus au comptoir,
   et « Suivi par lot » pour l'huile en vrac.
4. **Stock** : créer les lots si besoin, puis un mouvement « Stock initial » par article ou par lot.
5. **Paramètres › Taxes** et **Dépenses** : taux de TVA et catégories de dépenses.
6. **Finance et caisse › Caisse du jour** : ouvrir la caisse avec le fond du tiroir.
7. **Ventes › Vente rapide** : ajouter les produits, encaisser, valider (F9), imprimer.
8. **Achats** : créer une commande, la valider, réceptionner, payer.
9. **Rôles et permissions** : adapter les droits de chaque poste si besoin.
10. **Pressage client** : renseigner dans Paramètres le tarif au kilo et l'article « huile des clients »
    (article en vrac suivi par lot), puis enregistrer les réceptions d'olives.
11. **Production** : créer les articles Olives (suivis par lot), Huile en vrac (suivie par lot) et les sous-produits,
    acheter et réceptionner des olives, puis planifier une trituration.
12. **Conditionnement** : articles « Huile conditionnée » avec leur contenance en litres et articles « Emballage ».

## 7. Sauvegarde

Voir `docs/sauvegarde.md` : `./scripts/sauvegarde.sh` (à planifier chaque nuit) et `./scripts/restauration.sh`.

## 8. Suite du développement

| Phase | Contenu                                                                  |
|-------|--------------------------------------------------------------------------|
| 1     | Fondations (cette version)                                               |
| 2     | Paramètres de l'huilerie, clients, fournisseurs, catalogue, stock (lots, mouvements, inventaire) |
| 3     | Achats, réceptions, production et rendement                              |
| 4     | Pressage client avec stock séparé appartenant aux clients                |
| 5     | Conditionnement, vente rapide, historique et tickets 80 mm / 58 mm / A4 |
| 6     | Finance, caisse, dettes, tableau de bord calculé                        |

Voir `docs/` pour l'architecture, la base de données et les permissions.
