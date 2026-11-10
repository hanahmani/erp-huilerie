#!/usr/bin/env sh
# Restauration d'une sauvegarde : ./scripts/restauration.sh sauvegardes/huilerie_AAAAMMJJ_HHMMSS.sql.gz
# ATTENTION : remplace toutes les données actuelles. Arrêter le backend avant.
set -eu
[ $# -eq 1 ] || { echo "Usage : $0 fichier.sql.gz"; exit 1; }
cd "$(dirname "$0")/.."
docker compose stop backend
gunzip -c "$1" | docker compose exec -T mysql sh -c 'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
docker compose start backend
echo "Restauration terminée depuis $1"
