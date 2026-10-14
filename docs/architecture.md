# Architecture

Monolithe modulaire : une seule application Spring Boot, découpée en packages métier.

```
Navigateur ──> Nginx (frontend, port 80)
                 ├── /          fichiers React compilés
                 └── /api/*  ─> Spring Boot (backend, port 8080) ─> MySQL 8.4
```

## Backend `com.huilerie.erp`

| Package     | Rôle                                                                 |
|-------------|----------------------------------------------------------------------|
| `config`    | Configuration technique (clé et encodeur/décodeur JWT)               |
| `security`  | Chaîne de filtres, conversion JWT → utilisateur, création du 1er admin |
| `common`    | Erreurs (`GlobalExceptionHandler`), format `ApiError`, règles de validation |
| `auth`      | Connexion, profil courant, changement de mot de passe                 |
| `users`     | Utilisateurs, rôles, permissions                                      |
| `audit`     | Historique des actions sensibles                                      |
| `clients` … `dashboard` | Modules métier, vides à l'étape 1                         |

Chaque module suit la même structure : `Controller` (HTTP) → `Service` (règles, transactions,
`@PreAuthorize`) → `Repository` (JPA) → entités, avec `dto/`, `mapper/` et `enums/` si besoin.

## Format des erreurs

```json
{ "timestamp": "...", "status": 409, "error": "REGLE_METIER",
  "message": "Cet identifiant est déjà utilisé.", "path": "/api/users",
  "fieldErrors": { "username": "..." } }
```

Codes : 400 `VALIDATION`, 401 `NON_AUTHENTIFIE`, 403 `ACCES_REFUSE`, 404 `INTROUVABLE`,
409 `REGLE_METIER`, 500 `ERREUR_INTERNE` (détail uniquement dans les journaux serveur).

## Frontend

- `services/api.js` : client HTTP unique (jeton, erreurs, expiration de session).
- `context/AuthContext.jsx` : utilisateur connecté et ses permissions.
- `utils/navigation.js` : menu et description des modules.
- `components/` : boutons, champs, tableau, fenêtre modale, état vide, layout.
- `styles/` : variables de couleurs (vert olive, beige, blanc) et mise en page ; règles `@media print`.

## Règles pour les phases suivantes

- Montants et quantités : `BigDecimal` en Java, `DECIMAL(12,3)` en base.
- Le stock est la somme des mouvements ; aucune quantité modifiable directement.
- Ventes, paiements et sorties de stock validés dans une même transaction.
- L'huile des clients (pressage) a un propriétaire distinct et n'est jamais vendable.
