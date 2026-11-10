#!/usr/bin/env bash
# Sauvegarde de la base MySQL de l'ERP (données uniquement : toute la base huilerie_erp, triggers inclus).
# Usage : ./scripts/sauvegarde.sh            (planification : ./scripts/installer-sauvegarde.sh)
# Crée sauvegardes/huilerie_AAAAMMJJ_HHMMSS.sql.gz et conserve les 30 dernières sauvegardes valides.
# Un dump incomplet n'est jamais gardé : il ne peut donc pas remplacer une bonne sauvegarde dans la rotation.
set -euo pipefail
export PATH="/usr/local/bin:/usr/bin:/bin:${PATH:-}"   # cron démarre avec un PATH minimal
cd "$(dirname "$0")/.."

GARDER=30
mkdir -p sauvegardes
FICHIER="sauvegardes/huilerie_$(date +%Y%m%d_%H%M%S).sql.gz"
TMP="$FICHIER.partiel"
trap 'rm -f "$TMP"' EXIT

# MYSQL_PWD évite l'avertissement « password on the command line » ; les variables viennent du conteneur.
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --single-transaction \
    --routines --triggers --no-tablespaces -u root "$MYSQL_DATABASE"' | gzip > "$TMP"

# Contrôle : archive lisible et dump terminé (mysqldump écrit « -- Dump completed » en dernière ligne).
if ! gzip -t "$TMP" || ! gunzip -c "$TMP" | tail -n 1 | grep -q "Dump completed"; then
    echo "$(date '+%F %T') ÉCHEC : dump incomplet, aucune sauvegarde gardée." >&2
    exit 1
fi
mv "$TMP" "$FICHIER"
echo "$(date '+%F %T') Sauvegarde créée : $FICHIER ($(du -h "$FICHIER" | cut -f1))"

ls -1t sauvegardes/huilerie_*.sql.gz | tail -n +$((GARDER + 1)) | xargs -r rm --
