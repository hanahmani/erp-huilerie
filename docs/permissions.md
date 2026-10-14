# Rôles et permissions

Les permissions sont vérifiées par le backend avec `@PreAuthorize("hasAuthority('CODE')")`.
L'interface masque seulement les liens inaccessibles.

| Rôle              | Permissions principales |
|-------------------|-------------------------|
| `ADMIN`           | Toutes |
| `CAISSIER`        | DASHBOARD_VIEW, CLIENT_VIEW, CATALOG_VIEW, SALES_VIEW, SALES_CREATE |
| `RESP_STOCK`      | DASHBOARD_VIEW, CATALOG_VIEW/MANAGE, STOCK_VIEW/ADJUST, PURCHASE_VIEW, PACKAGING_VIEW |
| `RESP_PRODUCTION` | DASHBOARD_VIEW, CATALOG_VIEW, STOCK_VIEW, PRODUCTION_*, PRESSING_*, PACKAGING_* |
| `RESP_ACHATS`     | DASHBOARD_VIEW, SUPPLIER_*, CATALOG_VIEW, PURCHASE_VIEW/CREATE, STOCK_VIEW |
| `RESP_FINANCIER`  | DASHBOARD_VIEW, CLIENT_VIEW, SUPPLIER_VIEW, SALES_VIEW, PURCHASE_VIEW, FINANCE_VIEW, AUDIT_VIEW |

Permissions sensibles en caisse, réservées par défaut à l'administrateur :
`SALES_CANCEL`, `SALES_PRICE_OVERRIDE`, `SALES_DISCOUNT`.

## Règles particulières

- Création de compte : `USER_MANAGE` requis ; attribuer `ADMIN` exige d'être soi-même `ADMIN`.
- Un utilisateur ne peut pas désactiver son propre compte.
- Avec un mot de passe provisoire, l'utilisateur n'a aucune permission tant qu'il ne l'a pas changé.

## Endpoints de l'étape 1

| Méthode | Route                         | Accès                 |
|---------|-------------------------------|-----------------------|
| POST    | `/api/auth/login`             | public                |
| GET     | `/api/auth/me`                | connecté              |
| POST    | `/api/auth/change-password`   | connecté              |
| GET     | `/api/users`                  | USER_MANAGE           |
| POST    | `/api/users`                  | USER_MANAGE (+ ADMIN pour créer un admin) |
| PATCH   | `/api/users/{id}/status`      | USER_MANAGE           |
| GET     | `/api/roles`                  | USER_MANAGE           |
| GET     | `/actuator/health`            | public                |

## Endpoints ajoutés en v0.2

| Méthode | Route | Permission |
|---|---|---|
| GET / PUT | `/api/settings` | connecté / SETTINGS_MANAGE |
| GET | `/api/catalog/units`, `/api/catalog/categories` | connecté |
| POST, PUT, PATCH | `/api/catalog/units…`, `/categories…`, `/articles…` | CATALOG_MANAGE |
| GET | `/api/catalog/articles` | CATALOG_VIEW |
| GET / POST, PUT, PATCH | `/api/clients…` | CLIENT_VIEW / CLIENT_MANAGE |
| GET / POST, PUT, PATCH | `/api/suppliers…` | SUPPLIER_VIEW / SUPPLIER_MANAGE |
| GET | `/api/stock/state`, `/movements`, `/lots` | STOCK_VIEW |
| POST, PATCH | `/api/stock/movements`, `/lots`, `/lots/{id}/status` | STOCK_ADJUST |
| GET | `/api/sales/catalog` | SALES_CREATE |
| POST | `/api/sales/quick` | SALES_CREATE (+ SALES_DISCOUNT / SALES_PRICE_OVERRIDE selon la saisie) |
| GET, POST | `/api/sales`, `/{id}`, `/cashiers`, `/{id}/print` | SALES_VIEW |
| POST | `/api/sales/{id}/cancel` | SALES_CANCEL |
| GET | `/api/finance/cash` | FINANCE_VIEW |
| GET | `/api/dashboard` | DASHBOARD_VIEW (chaque bloc filtré selon les autres permissions) |

## Ajouts v0.3

Nouvelles permissions : `ROLE_MANAGE`, `PURCHASE_VALIDATE`, `PURCHASE_RECEIVE`, `CASH_SESSION`, `REPORT_VIEW`
(migration V6). Attribution initiale : ADMIN toutes ; CAISSIER `CASH_SESSION` ; RESP_STOCK `PURCHASE_RECEIVE`,
`REPORT_VIEW` ; RESP_ACHATS `PURCHASE_VALIDATE`, `PURCHASE_RECEIVE`, `REPORT_VIEW`, `FINANCE_VIEW` ;
RESP_PRODUCTION `REPORT_VIEW` ; RESP_FINANCIER `FINANCE_MANAGE`, `REPORT_VIEW`.

| Méthode | Route | Permission |
|---|---|---|
| GET, PUT, POST, DELETE | `/api/users/{id}`, `/reset-password` | USER_MANAGE |
| GET | `/api/permissions` | ROLE_MANAGE ou USER_MANAGE |
| POST, PUT, DELETE | `/api/roles…` | ROLE_MANAGE |
| GET | `/api/audit` | AUDIT_VIEW |
| GET / POST, PUT, PATCH | `/api/settings/taxes…` | connecté / SETTINGS_MANAGE |
| GET / POST, PUT, PATCH | `/api/settings/expense-categories…` | connecté / SETTINGS_MANAGE ou FINANCE_MANAGE |
| GET | `/api/purchases`, `/{id}` | PURCHASE_VIEW |
| GET | `/api/purchases/payables` | PURCHASE_VIEW ou FINANCE_VIEW |
| POST, PUT | `/api/purchases`, `/{id}` | PURCHASE_CREATE |
| POST | `/api/purchases/{id}/validate`, `/cancel` | PURCHASE_VALIDATE |
| POST | `/api/purchases/{id}/receive` | PURCHASE_RECEIVE |
| POST | `/api/purchases/{id}/pay` | FINANCE_MANAGE |
| GET / POST, PUT | `/api/stock/inventories…` | STOCK_VIEW / STOCK_ADJUST |
| GET / POST | `/api/finance/expenses…` | FINANCE_VIEW / FINANCE_MANAGE |
| GET / POST | `/api/finance/session…`, `/sessions` | CASH_SESSION ou FINANCE_VIEW / CASH_SESSION |
| GET | `/api/sales/receivables…` | SALES_VIEW ou FINANCE_VIEW |
| POST | `/api/sales/{id}/settle` | SALES_CREATE ou FINANCE_MANAGE |
| GET | `/api/reports/sales`, `/purchases`, `/stock`, `/finance` | REPORT_VIEW |

## Ajouts v0.4

| Méthode | Route | Permission |
|---|---|---|
| GET | `/api/pressings`, `/{id}` | PRESSING_VIEW |
| POST | `/api/pressings`, `/{id}/start`, `/finish`, `/return`, `/cancel` | PRESSING_MANAGE |
| POST | `/api/pressings/{id}/pay` | PRESSING_MANAGE ou SALES_CREATE |
| GET | `/api/production`, `/{id}` | PRODUCTION_VIEW |
| POST, PUT | `/api/production…`, `/start`, `/finish`, `/cancel` | PRODUCTION_MANAGE |
| GET | `/api/packaging` | PACKAGING_VIEW |
| POST | `/api/packaging`, `/{id}/cancel` | PACKAGING_MANAGE |
| GET | `/api/traceability/lots/{id}` | STOCK_VIEW ou PRODUCTION_VIEW |
| GET | `/api/reports/production` | REPORT_VIEW |

Le rôle CAISSIER reçoit `PRESSING_VIEW` (migration V7) pour pouvoir encaisser un pressage.

## Ajouts v0.5

Nouvelles permissions : `SALESDOC_MANAGE`, `INVOICE_VALIDATE`, `QUALITY_VIEW`, `QUALITY_MANAGE`, `HR_VIEW`, `HR_MANAGE`,
`LEAVE_REQUEST`, `LEAVE_VALIDATE`. Nouveaux rôles système : Responsable commercial, Responsable qualité, Responsable RH, Employé.
Tous les rôles existants reçoivent `LEAVE_REQUEST`.

| Route | Permission |
|---|---|
| `/api/sales-documents` (lecture) | SALES_VIEW |
| création, modification, validation, transformation, annulation | SALESDOC_MANAGE (+ INVOICE_VALIDATE pour valider une facture) |
| `/{id}/pay` | SALESDOC_MANAGE ou FINANCE_MANAGE |
| `/{id}/credit-note` | INVOICE_VALIDATE |
| `/api/quality/*` | QUALITY_VIEW / QUALITY_MANAGE |
| `/api/hr/employees`, `/absences`, `/services`, `/positions` | HR_VIEW / HR_MANAGE |
| `/api/hr/leaves` | LEAVE_REQUEST (ses demandes), HR_VIEW (toutes), LEAVE_VALIDATE (décision) |
| `/api/notifications`, `/api/search` | connecté (résultats filtrés selon les permissions) |
