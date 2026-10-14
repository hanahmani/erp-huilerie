# Base de données

MySQL 8.4, encodage `utf8mb4`, schéma géré exclusivement par Flyway
(`backend/src/main/resources/db/migration`). Hibernate est en mode `validate`.

## Migrations

| Version | Fichier                                  | Contenu                                    |
|---------|------------------------------------------|--------------------------------------------|
| V1      | `V1__securite_schema.sql`                | users, roles, permissions, user_roles, role_permissions, audit_logs |
| V2      | `V2__roles_permissions_techniques.sql`   | 27 permissions et 6 rôles (référentiel technique) |
| V3      | `V3__referentiels.sql`                   | paramètres, numérotation, unités, catégories, articles, clients, fournisseurs |
| V4      | `V4__stock.sql`                          | lots (propriétaire), projection du stock (CHECK ≥ 0), journal des mouvements |
| V5      | `V5__ventes_caisse.sql`                  | ventes, lignes, paiements, journal de caisse |
| V5.1    | `db/vendor/mysql/V5_1__…append_only.sql` | MySQL uniquement : triggers interdisant la modification des journaux |
| V6      | `V6__achats_finance_inventaire_parametrage.sql` | taxes, catégories de dépenses, rôles personnalisés, fiche utilisateur, achats, réceptions, paiements fournisseurs, règlements clients, dépenses, sessions de caisse, inventaires, 5 permissions |
| V6.1    | `db/vendor/mysql/V6_1__…append_only.sql` | MySQL : paiements et lignes de réception non modifiables |
| V7      | `V7__pressage_production_conditionnement.sql` | pressages, remises d'huile, paiements de pressage, ordres de production, entrées et sous-produits, conditionnements et emballages consommés, paramètres du pressage |

Règle : une migration appliquée n'est **jamais modifiée**. Toute évolution passe par un nouveau
fichier `V3__...sql`, `V4__...sql`, sans supprimer de données existantes.

## Tables de l'étape 1

- `users` : identifiant unique, prénom, nom, `password_hash` (BCrypt), `actif`,
  `must_change_password`, `token_version` (invalide les anciennes sessions).
- `roles`, `permissions`, `user_roles`, `role_permissions`.
- `audit_logs` : utilisateur, action (`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `USER_CREATE`,
  `USER_DEACTIVATE`, `PASSWORD_CHANGE`, `ADMIN_BOOTSTRAP`…), entité, date, détails.

## Sauvegarde (Docker)

```bash
docker compose exec mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" huilerie_erp' > sauvegarde.sql
```
