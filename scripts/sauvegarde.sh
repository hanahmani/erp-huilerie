#!/usr/bin/env sh
# Sauvegarde de la base MySQL de l'ERP (à planifier, ex. cron : 0 2 * * * /chemin/scripts/sauvegarde.sh)
# Conserve les 30 dernières sauvegardes compressées dans ./sauvegardes
set -eu
cd "$(dirname "$0")/.."
mkdir -p sauvegardes
FICHIER="sauvegardes/huilerie_$(date +%Y%m%d_%H%M%S).sql.gz"
docker compose exec -T mysql sh -c 'exec mysqldump --single-transaction --routines --triggers -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' | gzip > "$FICHIER"
echo "Sauvegarde créée : $FICHIER"
ls -1t sauvegardes/huilerie_*.sql.gz | tail -n +31 | xargs -r rm --
