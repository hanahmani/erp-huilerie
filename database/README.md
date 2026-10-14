# Base de données

Le schéma n'est pas créé à la main : il est appliqué automatiquement par **Flyway** au démarrage
du backend, à partir de `backend/src/main/resources/db/migration`.

- Avec Docker : la base `huilerie_erp` et l'utilisateur applicatif sont créés par le conteneur
  MySQL à partir du fichier `.env`.
- Sans Docker : exécuter le script SQL de création décrit dans le README principal.

Aucune donnée métier fictive n'est insérée. Voir `docs/database.md`.
