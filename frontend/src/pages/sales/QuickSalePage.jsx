import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { CheckCircle2, Minus, Plus, Printer, ReceiptText, ScanLine, ShoppingCart, Trash2, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useIdempotencyKey } from '../../hooks/useIdempotencyKey';
import { salesService } from '../../services/salesService';
import { clientService } from '../../services/clientService';
import { settingsService } from '../../services/settingsService';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import TicketPrintDialog from './TicketPrintDialog';
import SaleDetailModal from './SaleDetailModal';
import { formatDT, formatQty } from '../../utils/format';
import { PAYMENT_MODES } from '../../utils/labels';
import { fromMillimes, lineMillimes, normalizeDecimal, toMillimes } from '../../utils/money';

const MODES = Object.keys(PAYMENT_MODES);
const EMPTY_PAY = { ESPECES: '', CARTE: '', VIREMENT: '', CHEQUE: '' };

/** Quantité disponible pour une ligne (lot choisi ou total sans lot). */
function availableFor(article, lotId) {
  if (!article.suiviLot) return Number(article.disponible);
  const lot = article.lots.find((l) => String(l.lotId) === String(lotId));
  return lot ? Number(lot.quantite) : 0;
}

export default function QuickSalePage() {
  const { user, can } = useAuth();
  const canPrice = can('SALES_PRICE_OVERRIDE');
  const canDiscount = can('SALES_DISCOUNT');

  const [catalog, setCatalog] = useState(null);
  const [settings, setSettings] = useState(null);
  const [search, setSearch] = useState('');
  const [cart, setCart] = useState([]);           // { key, article, lotId, quantite, prix }
  const [clientQuery, setClientQuery] = useState('');
  const [clientOptions, setClientOptions] = useState([]);
  const [client, setClient] = useState(null);
  const [remise, setRemise] = useState('');
  const [pay, setPay] = useState(EMPTY_PAY);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(null);         // vente validée
  const [view, setView] = useState(null);         // 'print' | 'detail'
  const [confirmClear, setConfirmClear] = useState(false);
  const [idemKey, renewKey] = useIdempotencyKey();
  const searchRef = useRef(null);

  const loadCatalog = useCallback(() => salesService.catalog().then(setCatalog).catch((e) => setError(e.message)), []);
  useEffect(() => {
    loadCatalog();
    settingsService.get().then(setSettings).catch(() => setSettings({ configured: false }));
  }, [loadCatalog]);

  useEffect(() => {
    if (!can('CLIENT_VIEW') || clientQuery.trim().length < 2) { setClientOptions([]); return undefined; }
    const t = setTimeout(() => {
      clientService.list({ q: clientQuery, actif: true, size: 8 }).then((p) => setClientOptions(p.content)).catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [clientQuery, can]);

  // ---------------------------------------------------------------- panier
  const filtered = useMemo(() => {
    if (!catalog) return [];
    const q = search.trim().toLowerCase();
    if (!q) return catalog;
    return catalog.filter((a) => a.nom.toLowerCase().includes(q) || a.reference.toLowerCase().includes(q)
      || (a.codeBarres && a.codeBarres === search.trim()) || (a.categorie || '').toLowerCase().includes(q));
  }, [catalog, search]);

  const addArticle = (a) => {
    if (Number(a.disponible) <= 0) return;
    setError('');
    setCart((c) => {
      const lotId = a.suiviLot ? (a.lots[0]?.lotId ?? '') : '';
      const existing = c.find((l) => l.article.id === a.id && String(l.lotId) === String(lotId));
      if (existing) {
        return c.map((l) => (l === existing ? { ...l, quantite: fromMillimes(toMillimes(l.quantite) + 1000).replace(/\.?0+$/, '') } : l));
      }
      return [...c, { key: `${a.id}-${Date.now()}`, article: a, lotId, quantite: '1', prix: a.prixVente }];
    });
  };

  const updateLine = (key, patch) => setCart((c) => c.map((l) => (l.key === key ? { ...l, ...patch } : l)));
  const removeLine = (key) => setCart((c) => c.filter((l) => l.key !== key));
  const stepQty = (l, delta) => {
    const next = toMillimes(l.quantite) + delta * 1000;
    if (next > 0) updateLine(l.key, { quantite: fromMillimes(next).replace(/\.?0+$/, '') });
  };

  const onSearchKey = (e) => {
    if (e.key !== 'Enter') return;
    const code = search.trim();
    const exact = catalog?.find((a) => a.codeBarres === code || a.reference.toLowerCase() === code.toLowerCase());
    const target = exact || (filtered.length === 1 ? filtered[0] : null);
    if (target) {
      addArticle(target);
      setSearch('');
    }
  };

  // ---------------------------------------------------------------- calculs (aperçu ; le serveur recalcule)
  const lines = cart.map((l) => ({ ...l, totalM: lineMillimes(l.quantite, l.prix), dispo: availableFor(l.article, l.lotId) }));
  const sousTotalM = lines.reduce((s, l) => s + l.totalM, 0);
  const remiseM = Math.min(toMillimes(remise), sousTotalM);
  const totalM = sousTotalM - remiseM;
  const especesM = toMillimes(pay.ESPECES);
  const autresM = MODES.filter((m) => m !== 'ESPECES').reduce((s, m) => s + toMillimes(pay[m]), 0);
  const recuM = especesM + autresM;
  const monnaieM = Math.max(0, recuM - totalM);
  const resteM = Math.max(0, totalM - recuM);
  const creditOk = Boolean(settings?.venteCreditAutorisee && client);

  const problems = [];
  if (!cart.length) problems.push('Ajoutez au moins un produit.');
  lines.forEach((l) => {
    if (toMillimes(l.quantite) <= 0) problems.push(`Quantité invalide pour ${l.article.nom}.`);
    else if (l.article.suiviLot && !l.lotId) problems.push(`Choisissez le lot de ${l.article.nom}.`);
    else if (toMillimes(l.quantite) > Math.round(l.dispo * 1000)) problems.push(`Stock insuffisant pour ${l.article.nom} (${formatQty(l.dispo, l.article.unite)}).`);
  });
  if (autresM > totalM) problems.push('Carte, virement et chèque ne peuvent pas dépasser le total.');
  if (cart.length && resteM > 0 && !creditOk) {
    problems.push(client ? 'Paiement incomplet : les ventes à crédit sont désactivées dans les paramètres.'
      : 'Paiement incomplet : sélectionnez un client pour une vente à crédit.');
  }

  // ---------------------------------------------------------------- actions
  const reset = () => {
    setCart([]);
    setRemise('');
    setPay(EMPTY_PAY);
    setClient(null);
    setClientQuery('');
    setError('');
    renewKey();
    setTimeout(() => searchRef.current?.focus(), 0);
  };

  const validate = async () => {
    if (problems.length || submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const sale = await salesService.quickSale({
        idempotencyKey: idemKey,
        clientId: client?.id ?? null,
        remise: remiseM ? fromMillimes(remiseM) : null,
        lignes: cart.map((l) => ({
          articleId: l.article.id,
          lotId: l.lotId ? Number(l.lotId) : null,
          quantite: normalizeDecimal(l.quantite),
          prixUnitaire: canPrice ? normalizeDecimal(l.prix) : null,
        })),
        paiements: MODES.filter((m) => toMillimes(pay[m]) > 0).map((m) => ({ mode: m, montant: normalizeDecimal(pay[m]) })),
      });
      setDone(sale);
      renewKey();
      loadCatalog();
    } catch (err) {
      setError(err.message);
      if (err.status === 409) loadCatalog();
    } finally {
      setSubmitting(false);
    }
  };

  const newSale = () => {
    setDone(null);
    setView(null);
    reset();
  };

  // Raccourci F9 : valider
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'F9') { e.preventDefault(); validate(); } };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  if (!catalog) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  return (
    <div className="pos">
      {/* --------------------------------------------------------- produits */}
      <section className="pos-products">
        <div className="pos-head">
          <div>
            <h1>Vente rapide</h1>
            <p className="muted">Caissier : {user.firstName} {user.lastName}</p>
          </div>
        </div>
        <label className="search-box pos-search">
          <ScanLine size={18} />
          <input ref={searchRef} autoFocus value={search} onChange={(e) => setSearch(e.target.value)} onKeyDown={onSearchKey}
                 placeholder="Nom, référence ou code-barres puis Entrée" aria-label="Rechercher un produit" />
        </label>

        {!catalog.length ? (
          <div className="panel">
            <EmptyState icon={ShoppingCart} title="Aucun produit vendable"
                        text="Dans le catalogue, cochez « Vendable en caisse » et indiquez un prix de vente, puis saisissez le stock." />
          </div>
        ) : (
          <div className="pos-grid">
            {filtered.map((a) => {
              const empty = Number(a.disponible) <= 0;
              return (
                <button key={a.id} type="button" className="pos-tile" disabled={empty} onClick={() => addArticle(a)}>
                  <span className="pos-tile-name">{a.nom}</span>
                  <span className="pos-tile-meta">{a.reference}{a.categorie ? ` · ${a.categorie}` : ''}</span>
                  <span className="pos-tile-foot">
                    <strong>{formatDT(a.prixVente)}</strong>
                    <span className={empty ? 'pos-stock-out' : 'pos-stock'}>{empty ? 'Rupture' : formatQty(a.disponible, a.unite)}</span>
                  </span>
                </button>
              );
            })}
            {!filtered.length ? <p className="muted">Aucun produit ne correspond.</p> : null}
          </div>
        )}
      </section>

      {/* --------------------------------------------------------- panier */}
      <aside className="pos-cart panel">
        <div className="pos-client">
          {client ? (
            <div className="pos-client-chip">
              <span><strong>{client.nom}</strong> <span className="muted">{client.code}</span></span>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setClient(null)} aria-label="Retirer le client"><X size={16} /></button>
            </div>
          ) : can('CLIENT_VIEW') ? (
            <div className="pos-client-search">
              <input className="input" value={clientQuery} onChange={(e) => setClientQuery(e.target.value)}
                     placeholder="Client comptoir · rechercher un client" />
              {clientOptions.length ? (
                <ul className="pos-suggest">
                  {clientOptions.map((c) => (
                    <li key={c.id}><button type="button" onClick={() => { setClient(c); setClientQuery(''); setClientOptions([]); }}>
                      {c.nom} <span className="muted">{c.code}{c.telephone ? ` · ${c.telephone}` : ''}</span>
                    </button></li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : <span className="muted">Client comptoir</span>}
        </div>

        <div className="pos-lines">
          {!lines.length ? <p className="muted pos-empty">Le panier est vide. Touchez un produit pour l'ajouter.</p> : lines.map((l) => (
            <div key={l.key} className="pos-line">
              <div className="pos-line-top">
                <strong>{l.article.nom}</strong>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => removeLine(l.key)} aria-label="Supprimer la ligne"><Trash2 size={16} /></button>
              </div>
              {l.article.suiviLot ? (
                <select className="select pos-lot" value={l.lotId} onChange={(e) => updateLine(l.key, { lotId: e.target.value })}>
                  <option value="">Choisir le lot…</option>
                  {l.article.lots.map((lot) => <option key={lot.lotId} value={lot.lotId}>Lot {lot.numero} · {formatQty(lot.quantite, l.article.unite)}</option>)}
                </select>
              ) : null}
              <div className="pos-line-row">
                <div className="qty">
                  <button type="button" onClick={() => stepQty(l, -1)} aria-label="Diminuer"><Minus size={14} /></button>
                  <input inputMode="decimal" value={l.quantite} onChange={(e) => updateLine(l.key, { quantite: e.target.value })} aria-label="Quantité" />
                  <button type="button" onClick={() => stepQty(l, 1)} aria-label="Augmenter"><Plus size={14} /></button>
                </div>
                <span className="muted">{l.article.unite} ×</span>
                {canPrice
                  ? <input className="input pos-price" inputMode="decimal" value={l.prix} onChange={(e) => updateLine(l.key, { prix: e.target.value })} aria-label="Prix unitaire" />
                  : <span>{formatDT(l.prix)}</span>}
                <strong className="pos-line-total">{formatDT(fromMillimes(l.totalM))}</strong>
              </div>
            </div>
          ))}
        </div>

        <div className="pos-sum">
          <div><span>Sous-total</span><span>{formatDT(fromMillimes(sousTotalM))}</span></div>
          {canDiscount ? (
            <div><span>Remise</span><input className="input pos-mini" inputMode="decimal" value={remise} onChange={(e) => setRemise(e.target.value)} placeholder="0,000" /></div>
          ) : null}
          <div className="pos-total"><span>Total</span><span>{formatDT(fromMillimes(totalM))}</span></div>
        </div>

        <div className="pos-pay">
          {MODES.map((m) => (
            <label key={m} className="pos-pay-field">
              <span>{PAYMENT_MODES[m]}</span>
              <input className="input" inputMode="decimal" value={pay[m]} placeholder="0,000"
                     onChange={(e) => setPay((p) => ({ ...p, [m]: e.target.value }))} />
            </label>
          ))}
          <Button size="sm" variant="secondary" disabled={!totalM}
                  onClick={() => setPay({ ...EMPTY_PAY, ESPECES: fromMillimes(totalM) })}>Espèces : montant exact</Button>
        </div>

        <div className="pos-sum">
          <div><span>Montant payé</span><span>{formatDT(fromMillimes(Math.min(recuM, totalM)))}</span></div>
          {resteM > 0 ? <div className="pos-reste"><span>Reste à payer</span><span>{formatDT(fromMillimes(resteM))}</span></div> : null}
          {monnaieM > 0 ? <div className="pos-change"><span>Monnaie à rendre</span><span>{formatDT(fromMillimes(monnaieM))}</span></div> : null}
        </div>

        <Alert type="error">{error}</Alert>
        {cart.length && problems.length ? <p className="pos-problem">{problems[0]}</p> : null}

        <div className="pos-actions">
          <Button variant="secondary" disabled={!cart.length || submitting} onClick={() => setConfirmClear(true)}>Annuler la vente</Button>
          <Button onClick={validate} loading={submitting} disabled={Boolean(problems.length)}>Valider la vente (F9)</Button>
        </div>
      </aside>

      {confirmClear ? (
        <ConfirmDialog title="Annuler cette vente ?" message="Le panier sera vidé. Rien n'a encore été enregistré."
                       confirmLabel="Vider le panier" danger onCancel={() => setConfirmClear(false)}
                       onConfirm={() => { setConfirmClear(false); reset(); }} />
      ) : null}

      {done && !view ? (
        <Modal title="Vente validée" onClose={newSale}>
          <div className="pos-done">
            <CheckCircle2 size={44} />
            <strong>{done.numeroTicket}</strong>
            <span>Total {formatDT(done.total)}</span>
            {Number(done.monnaieRendue) > 0 ? <span className="pos-done-change">Monnaie à rendre : {formatDT(done.monnaieRendue)}</span> : null}
            {Number(done.resteAPayer) > 0 ? <span>Reste à payer : {formatDT(done.resteAPayer)}</span> : null}
          </div>
          <div className="pos-done-actions">
            <Button onClick={() => setView('print')}><Printer size={18} />Imprimer le ticket</Button>
            <Button variant="secondary" onClick={newSale}><Plus size={18} />Nouvelle vente</Button>
            <Button variant="secondary" onClick={() => setView('detail')}><ReceiptText size={18} />Consulter la vente</Button>
          </div>
        </Modal>
      ) : null}
      {done && view === 'print' ? <TicketPrintDialog sale={done} onClose={newSale} onPrinted={setDone} /> : null}
      {done && view === 'detail' ? <SaleDetailModal saleId={done.id} onClose={() => { setView(null); }} /> : null}
    </div>
  );
}
