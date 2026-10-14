import { useCallback, useEffect, useState } from 'react';
import { Download, Printer } from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Spinner from '../../components/common/Spinner';
import StatCard from '../../components/common/StatCard';
import { DateInput } from '../../components/tables/Toolbar';
import { reportService } from '../../services/reportService';
import { formatDT, formatQty, todayISO } from '../../utils/format';
import { ARTICLE_TYPES, OIL_CATEGORIES, PAYMENT_MODES } from '../../utils/labels';
import { exportCsv } from '../../utils/exportCsv';

const firstOfMonth = () => `${todayISO().slice(0, 8)}01`;
const n = (v) => Number(v ?? 0);

/** Tableau de rapport avec export CSV. cols : [{ header, render, csv, num }] */
function ReportTable({ title, rows, cols, file, empty = 'Aucune donnée sur la période.' }) {
  return (
    <div className="panel report-block">
      <div className="section-title">
        <h2>{title}</h2>
        {rows.length ? (
          <Button size="sm" variant="secondary" className="no-print"
                  onClick={() => exportCsv(file, cols.map((c) => ({ header: c.header, value: c.csv })), rows)}>
            <Download size={16} />CSV
          </Button>
        ) : null}
      </div>
      {rows.length ? (
        <div className="table-wrap">
          <table className="table">
            <thead><tr>{cols.map((c) => <th key={c.header} className={c.num ? 'num' : undefined}>{c.header}</th>)}</tr></thead>
            <tbody>{rows.map((r, i) => (
              <tr key={i}>{cols.map((c) => <td key={c.header} className={c.num ? 'num' : undefined}>{c.render(r)}</td>)}</tr>
            ))}</tbody>
          </table>
        </div>
      ) : <div className="panel-body muted">{empty}</div>}
    </div>
  );
}

function Bars({ data }) {
  const max = Math.max(...data.map((d) => n(d.total)), 0);
  if (!max) return <div className="panel-body muted">Aucune vente sur la période.</div>;
  return (
    <div className="panel-body">
      <div className="bars" role="img" aria-label="Ventes par jour">
        {data.map((d) => (
          <div key={d.jour} className="bars-col" title={`${new Date(`${d.jour}T12:00:00`).toLocaleDateString('fr-FR')} : ${formatDT(d.total)}`}>
            <div className="bars-fill" style={{ height: `${(n(d.total) / max) * 100}%` }} />
          </div>
        ))}
      </div>
      <div className="bars-axis"><span>{new Date(`${data[0].jour}T12:00:00`).toLocaleDateString('fr-FR')}</span>
        <span>{new Date(`${data[data.length - 1].jour}T12:00:00`).toLocaleDateString('fr-FR')}</span></div>
    </div>
  );
}

function SalesReport({ r }) {
  return (
    <>
      <div className="stats">
        <StatCard label="Chiffre d'affaires TTC" value={formatDT(r.chiffreAffaires)} />
        <StatCard label="Nombre de ventes" value={r.nbVentes} />
        <StatCard label="Panier moyen" value={formatDT(r.panierMoyen)} />
        <StatCard label="Remises accordées" value={formatDT(r.remises)} />
        <StatCard label="Ventes annulées" value={r.nbAnnulations} tone={r.nbAnnulations ? 'warn' : undefined} />
      </div>
      <div className="panel report-block"><div className="section-title"><h2>Ventes par jour</h2>
        <Button size="sm" variant="secondary" className="no-print" onClick={() => exportCsv('ventes-par-jour', [
          { header: 'Jour', value: (d) => d.jour }, { header: 'Ventes', value: (d) => d.nombre }, { header: 'Total', value: (d) => n(d.total) },
        ], r.parJour)}><Download size={16} />CSV</Button></div><Bars data={r.parJour} /></div>
      <div className="grid-2">
        <ReportTable title="Par article" file="ventes-par-article" rows={r.parArticle} cols={[
          { header: 'Article', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Quantité', num: true, render: (x) => formatQty(x.quantite, x.unite), csv: (x) => n(x.quantite) },
          { header: 'Montant', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
        ]} />
        <ReportTable title="Par client" file="ventes-par-client" rows={r.parClient} cols={[
          { header: 'Client', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Ventes', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Montant', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
          { header: 'Reste dû', num: true, render: (x) => formatDT(x.montant2), csv: (x) => n(x.montant2) },
        ]} />
        <ReportTable title="Par caissier" file="ventes-par-caissier" rows={r.parCaissier} cols={[
          { header: 'Caissier', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Ventes', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Montant', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
        ]} />
      </div>
    </>
  );
}

function PurchasesReport({ r }) {
  return (
    <>
      <div className="stats">
        <StatCard label="Achats engagés TTC" value={formatDT(r.totalTtc)} />
        <StatCard label="Dettes fournisseurs (à ce jour)" value={formatDT(r.dettesFournisseurs)} tone={n(r.dettesFournisseurs) ? 'warn' : undefined} />
      </div>
      <ReportTable title="Achats par fournisseur" file="achats-par-fournisseur" rows={r.parFournisseur} cols={[
        { header: 'Fournisseur', render: (x) => x.libelle, csv: (x) => x.libelle },
        { header: 'Commandes', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
        { header: 'Total HT', num: true, render: (x) => formatDT(x.quantite), csv: (x) => n(x.quantite) },
        { header: 'Total TTC', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
        { header: 'Payé', num: true, render: (x) => formatDT(x.montant2), csv: (x) => n(x.montant2) },
      ]} />
    </>
  );
}

function StockReport({ r }) {
  return (
    <>
      <div className="stats">
        <StatCard label="Valeur du stock disponible" value={formatDT(r.valeurTotale)} hint="Au dernier prix d'achat saisi" />
        <StatCard label="Articles" value={r.nbArticles} />
        <StatCard label="Sous le seuil d'alerte" value={r.nbAlertes} tone={r.nbAlertes ? 'warn' : undefined} />
        <StatCard label="En stock sans prix d'achat" value={r.nbSansPrix} tone={r.nbSansPrix ? 'warn' : undefined} hint="Non valorisés" />
      </div>
      <ReportTable title="Stock valorisé" file="stock-valorise" rows={r.lignes} empty="Aucun article." cols={[
        { header: 'Référence', render: (x) => x.reference, csv: (x) => x.reference },
        { header: 'Article', render: (x) => <>{x.nom}{x.alerte ? <span className="badge badge-warn" style={{ marginLeft: 6 }}>Faible</span> : null}</>, csv: (x) => x.nom },
        { header: 'Type', render: (x) => ARTICLE_TYPES[x.type], csv: (x) => ARTICLE_TYPES[x.type] },
        { header: 'Disponible', num: true, render: (x) => formatQty(x.disponible, x.unite), csv: (x) => n(x.disponible) },
        { header: "Prix d'achat", num: true, render: (x) => (x.prixAchat != null ? formatDT(x.prixAchat) : '—'), csv: (x) => (x.prixAchat != null ? n(x.prixAchat) : '') },
        { header: 'Valeur', num: true, render: (x) => (x.valeur != null ? formatDT(x.valeur) : '—'), csv: (x) => (x.valeur != null ? n(x.valeur) : '') },
      ]} />
    </>
  );
}

function FinanceReport({ r }) {
  return (
    <>
      <div className="stats">
        <StatCard label="Encaissements" value={formatDT(r.encaissements)} />
        <StatCard label="Décaissements" value={formatDT(r.decaissements)} />
        <StatCard label="Solde de trésorerie" value={formatDT(r.solde)} />
        <StatCard label="Dépenses" value={formatDT(r.depenses)} />
        <StatCard label="Créances clients" value={formatDT(r.creancesClients)} hint="À ce jour" />
        <StatCard label="Dettes fournisseurs" value={formatDT(r.dettesFournisseurs)} hint="À ce jour" />
      </div>
      <div className="grid-2">
        <ReportTable title="Par moyen de paiement" file="tresorerie-par-mode" rows={r.parMode.filter((x) => n(x.montant) || n(x.montant2))} cols={[
          { header: 'Mode', render: (x) => PAYMENT_MODES[x.libelle], csv: (x) => PAYMENT_MODES[x.libelle] },
          { header: 'Encaissé', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
          { header: 'Décaissé', num: true, render: (x) => formatDT(x.montant2), csv: (x) => n(x.montant2) },
        ]} />
        <ReportTable title="Dépenses par catégorie" file="depenses-par-categorie" rows={r.depensesParCategorie} cols={[
          { header: 'Catégorie', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Nombre', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Montant', num: true, render: (x) => formatDT(x.montant), csv: (x) => n(x.montant) },
        ]} />
      </div>
    </>
  );
}

function ProductionReport({ r }) {
  return (
    <>
      <h2 style={{ marginBottom: 10 }}>Trituration des olives de l'huilerie</h2>
      <div className="stats">
        <StatCard label="Triturations terminées" value={r.nbTriturations} />
        <StatCard label="Olives triturées" value={formatQty(r.olivesKg, 'kg')} />
        <StatCard label="Huile produite" value={formatQty(r.huileLitres, 'L')} />
        <StatCard label="Rendement moyen" value={`${Number(r.rendementMoyen).toFixed(2)} L/100 kg`} />
      </div>
      <h2 style={{ marginBottom: 10 }}>Pressage pour les clients</h2>
      <div className="stats">
        <StatCard label="Pressages" value={r.nbPressages} />
        <StatCard label="Olives reçues (net)" value={formatQty(r.pressageKg, 'kg')} />
        <StatCard label="Huile obtenue" value={formatQty(r.pressageHuile, 'L')} />
        <StatCard label="Rendement moyen" value={`${Number(r.pressageRendement).toFixed(2)} L/100 kg`} />
        <StatCard label="Chiffre du service" value={formatDT(r.pressageChiffre)} />
      </div>
      <div className="grid-2">
        <ReportTable title="Huile produite par catégorie" file="production-par-categorie" rows={r.parCategorie} cols={[
          { header: 'Catégorie', render: (x) => OIL_CATEGORIES[x.libelle] || x.libelle, csv: (x) => OIL_CATEGORIES[x.libelle] || x.libelle },
          { header: 'Lots', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Huile', num: true, render: (x) => formatQty(x.quantite, 'L'), csv: (x) => n(x.quantite) },
        ]} />
        <ReportTable title="Rendement par variété (pressage)" file="rendement-par-variete" rows={r.rendementParVariete} cols={[
          { header: 'Variété', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Pressages', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Olives (kg)', num: true, render: (x) => formatQty(x.montant), csv: (x) => n(x.montant) },
          { header: 'Huile (L)', num: true, render: (x) => formatQty(x.montant2), csv: (x) => n(x.montant2) },
          { header: 'L/100 kg', num: true, render: (x) => Number(x.quantite).toFixed(2), csv: (x) => n(x.quantite) },
        ]} />
        <ReportTable title="Conditionnement par produit" file="conditionnement" rows={r.conditionnement} cols={[
          { header: 'Produit', render: (x) => x.libelle, csv: (x) => x.libelle },
          { header: 'Opérations', num: true, render: (x) => x.nombre, csv: (x) => x.nombre },
          { header: 'Quantité', num: true, render: (x) => formatQty(x.quantite), csv: (x) => n(x.quantite) },
          { header: 'Vrac utilisé (L)', num: true, render: (x) => formatQty(x.montant), csv: (x) => n(x.montant) },
        ]} />
      </div>
    </>
  );
}

const TYPES = [
  { value: 'sales', label: 'Ventes' },
  { value: 'purchases', label: 'Achats' },
  { value: 'production', label: 'Production et pressage' },
  { value: 'stock', label: 'Stock' },
  { value: 'finance', label: 'Finance' },
];

export default function ReportsPage() {
  const [type, setType] = useState('sales');
  const [from, setFrom] = useState(firstOfMonth());
  const [to, setTo] = useState(todayISO());
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    setData(null);
    try {
      setData(type === 'stock' ? await reportService.stock() : await reportService[type](from, to));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [type, from, to]);
  useEffect(() => { load(); }, [load]);

  return (
    <>
      <PageHeader title="Rapports et statistiques" description="Calculés à partir des opérations enregistrées. Export CSV pour Excel, impression pour un PDF."
                  actions={<Button variant="secondary" onClick={() => window.print()} className="no-print"><Printer size={18} />Imprimer</Button>} />
      <div className="no-print">
        <Tabs value={type} onChange={setType} tabs={TYPES} />
        {type !== 'stock' ? (
          <div className="toolbar panel" style={{ marginBottom: 18 }}>
            <DateInput label="Du" value={from} onChange={setFrom} />
            <DateInput label="Au" value={to} onChange={setTo} />
            <Button size="sm" variant="secondary" onClick={() => { setFrom(todayISO()); setTo(todayISO()); }}>Aujourd'hui</Button>
            <Button size="sm" variant="secondary" onClick={() => { setFrom(firstOfMonth()); setTo(todayISO()); }}>Ce mois</Button>
            <Button size="sm" variant="secondary" onClick={() => { setFrom(`${todayISO().slice(0, 4)}-01-01`); setTo(todayISO()); }}>Cette année</Button>
          </div>
        ) : null}
      </div>
      <h2 className="print-only">{TYPES.find((t) => t.value === type).label}{type !== 'stock' ? ` du ${from} au ${to}` : ''}</h2>
      <Alert type="error">{error}</Alert>
      {loading ? <Spinner /> : null}
      {data && type === 'sales' ? <SalesReport r={data} /> : null}
      {data && type === 'purchases' ? <PurchasesReport r={data} /> : null}
      {data && type === 'stock' ? <StockReport r={data} /> : null}
      {data && type === 'production' ? <ProductionReport r={data} /> : null}
      {data && type === 'finance' ? <FinanceReport r={data} /> : null}
    </>
  );
}
