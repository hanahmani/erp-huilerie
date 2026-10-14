# Décisions de conception

1. **Stock = somme des mouvements.** `stock_movements` est le journal ; `stock_balances` est une projection
   mise à jour dans la même transaction, avec `CHECK (quantite >= 0)`. La ligne de stock est verrouillée
   (`SELECT … FOR UPDATE`) : deux caisses ne peuvent pas vendre le même litre.
2. **Journaux non modifiables.** Sous MySQL, des triggers (`db/vendor/mysql`) interdisent UPDATE/DELETE sur
   `stock_movements`, `cash_movements`, `audit_logs` et `sale_lines`. Les corrections passent par des
   mouvements compensatoires.
3. **Huile des clients.** Un lot a un propriétaire `ENTREPRISE` ou `CLIENT` (contrainte CHECK). Seuls les lots
   `ENTREPRISE` + `DISPONIBLE` sont proposés et acceptés à la vente. L'huile des clients est toujours suivie par lot.
4. **Numérotation sans trou.** `document_sequences` est verrouillée dans la transaction du document : une vente
   échouée ne consomme pas de numéro (`TCK-AAAA-000001`). Même principe pour `CLI-000001` et `FRN-000001`.
5. **Double soumission.** Chaque vente porte une `idempotency_key` unique générée par l'écran de caisse :
   un double clic ou un nouvel envoi renvoie la vente déjà créée.
6. **Impression.** Imprimer ou réimprimer appelle `/print` : compteur et audit mis à jour, jamais le stock ni
   la caisse. À partir de la 2e impression, le ticket porte la mention DUPLICATA.
7. **Paiements.** Seules les espèces peuvent dépasser le total (monnaie rendue). Le crédit (reste > 0) exige
   un client identifié, l'option « ventes à crédit » activée dans les paramètres et le respect du plafond.
8. **Prix et remise.** Le prix catalogue s'applique sauf permission `SALES_PRICE_OVERRIDE` ; la remise exige
   `SALES_DISCOUNT`. Le serveur recalcule tout ; le frontend n'affiche qu'un aperçu.
9. **Moyens de paiement** : liste fixe dans le code (espèces, carte, virement, chèque) plutôt qu'une table,
   pour ne créer aucune donnée initiale.
10. **Logo des tickets** : fichier statique `frontend/public/logo/logo.png`, pas stocké en base.
11. **Hibernate** : enums stockés en VARCHAR (`@JdbcTypeCode(VARCHAR)`) pour rester compatibles avec
    `ddl-auto=validate` ; `Instant` mappé en `DATETIME(6)`.
12. **Un seul dépôt.** Le stock n'est pas ventilé par dépôt. Ajouter des dépôts demandera une colonne `depot_id`
    dans `stock_balances` et `stock_movements` (nouvelle migration), sans changer le principe.
13. **Achats.** Le numéro `ACH-AAAA-…` est attribué dès le brouillon ; une commande n'est jamais supprimée, seulement
    annulée (avant toute réception et tout paiement). La « facture fournisseur » est la commande validée avec sa
    référence de facture ; le paiement est plafonné au reste dû.
14. **Réception d'un article suivi par lot** : le numéro de lot est obligatoire ; le lot est créé (propriétaire
    ENTREPRISE) ou complété s'il existe déjà.
15. **Inventaire.** La validation ramène chaque ligne comptée au comptage en tenant compte des mouvements survenus
    pendant le comptage (écart recalculé sur le stock du moment). Les lignes laissées vides ne sont pas ajustées.
16. **Caisse.** Une seule session ouverte à la fois (index unique sur `ouverte_flag`). Les ventes restent possibles
    sans session ouverte ; la session sert au contrôle des espèces (fond + entrées − sorties).
17. **Dépenses** : annulation par contre-passation (encaissement de même montant), jamais par suppression.
18. **Ventes à crédit** : un règlement ultérieur réduit le reste dû sans changer le total ; une vente réglée
    ultérieurement ne peut plus être annulée.
19. **Rapports** : calculés à la demande (période limitée à 400 jours), regroupés par jour dans le fuseau Africa/Tunis.
    Export CSV compatible Excel (séparateur « ; »), PDF via l'impression du navigateur.
20. **Valorisation du stock** au dernier prix d'achat saisi sur l'article ; les articles sans prix sont signalés.
21. **Pressage client** : facturé au kilo d'olives net (tarif par défaut dans les paramètres). L'huile obtenue entre
    dans un lot `CLIENT` portant le numéro du pressage. La remise de l'huile exige que le service soit entièrement
    payé. L'option « part d'huile retenue en paiement » n'est pas encore gérée.
22. **Production** : seules les olives de l'huilerie (lots `ENTREPRISE`) sont triturées ici. Les olives sortent du
    stock au démarrage ; l'huile et les sous-produits entrent à la fin. Rendement = litres pour 100 kg d'olives.
    Le classement par acidité (≤ 0,8 / ≤ 2 / ≤ 3,3 %) est indicatif ; le lot peut rester bloqué jusqu'à l'analyse.
23. **Conditionnement** : volume de vrac = quantité × contenance de l'article fini. Le produit fini suivi par lot
    reprend le numéro du lot de vrac. Annulation par mouvements `ANNULATION_PLUS/MOINS`, refusée si les produits
    ont déjà été vendus (stock insuffisant).
