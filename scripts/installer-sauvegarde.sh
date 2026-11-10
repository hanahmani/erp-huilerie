#!/usr/bin/env bash
# Planifie la sauvegarde de la base chaque nuit à 2 h (crontab de l'utilisateur courant).
# Usage sur le serveur : ./scripts/installer-sauvegarde.sh
# Relançable sans risque : l'ancienne ligne de sauvegarde est remplacée, les autres tâches cron sont conservées.
set -euo pipefail
cd "$(dirname "$0")/.."
DIR="$(pwd)"
HEURE="${HEURE_SAUVEGARDE:-0 2}"          # « minute heure » ; ex. HEURE_SAUVEGARDE="30 3" pour 3 h 30

chmod +x scripts/sauvegarde.sh scripts/restauration.sh
mkdir -p sauvegardes logs

LIGNE="$HEURE * * * $DIR/scripts/sauvegarde.sh >> $DIR/logs/sauvegarde.log 2>&1"
{ crontab -l 2>/dev/null || true; } | { grep -vF "$DIR/scripts/sauvegarde.sh" || true; } > /tmp/crontab.$$
echo "$LIGNE" >> /tmp/crontab.$$
crontab /tmp/crontab.$$
rm -f /tmp/crontab.$$

echo "Tâche cron installée :"
crontab -l | grep -F "$DIR/scripts/sauvegarde.sh"
echo
echo "Test immédiat de la sauvegarde :"
./scripts/sauvegarde.sh
