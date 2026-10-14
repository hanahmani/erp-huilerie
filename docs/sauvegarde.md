# Sauvegarde et restauration

- **Sauvegarder** : `./scripts/sauvegarde.sh` (dump cohérent `--single-transaction`, triggers inclus, compressé,
  30 dernières copies gardées dans `sauvegardes/`). À planifier chaque nuit (cron ou planificateur Windows).
- **Copier hors du serveur** : recopier régulièrement le dossier `sauvegardes/` sur un autre support (disque externe, stockage distant).
- **Restaurer** : `./scripts/restauration.sh sauvegardes/huilerie_AAAAMMJJ_HHMMSS.sql.gz` (arrête le backend,
  remplace les données, redémarre).
- **Tester** : restaurer une sauvegarde au moins une fois par mois sur un poste de test (`docker compose -p test up`)
  pour vérifier qu'elle est exploitable.
