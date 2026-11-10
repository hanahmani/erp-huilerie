# Sauvegarde et restauration

- **Planifier (une fois, sur le serveur)** : `./scripts/installer-sauvegarde.sh`. Installe une tâche cron qui sauvegarde
  la base chaque nuit à 2 h (autre heure : `HEURE_SAUVEGARDE="30 3" ./scripts/installer-sauvegarde.sh`), puis lance une
  première sauvegarde pour vérifier. Relançable sans risque.
- **Sauvegarder à la main** : `./scripts/sauvegarde.sh`. Dump de toute la base (`--single-transaction`, triggers inclus),
  compressé dans `sauvegardes/huilerie_AAAAMMJJ_HHMMSS.sql.gz` ; les 30 dernières sont gardées. Un dump incomplet est
  supprimé et le script sort en erreur : il ne remplace jamais une bonne sauvegarde.
- **Vérifier** : `tail logs/sauvegarde.log` (une ligne par nuit : fichier créé ou échec).
- **Copier hors du serveur** : recopier régulièrement `sauvegardes/` ailleurs (PC, disque externe, stockage distant),
  par exemple depuis un PC : `rsync -av utilisateur@serveur:apps/huilerie-erp/sauvegardes/ ~/sauvegardes-huilerie/`.
- **Restaurer** : `./scripts/restauration.sh sauvegardes/huilerie_AAAAMMJJ_HHMMSS.sql.gz` (arrête le backend,
  remplace les données, redémarre).
- **Tester** : restaurer une sauvegarde au moins une fois par mois sur un poste de test, jamais sur la production.
