import { useState } from 'react';
import { Boxes, Lock, LockOpen, Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { stockService } from '../../services/stockService';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import MovementModal from './MovementModal';
import LotModal from './LotModal';
import InventoriesTab from './InventoriesTab';
import TraceModal from './TraceModal';
import { ARTICLE_TYPES, LOT_OWNERS, LOT_STATUS, MOVEMENT_TYPES, toOptions } from '../../utils/labels';
import { dayRange, formatDateTime, formatQty } from '../../utils/format';
import { exportCsv } from '../../utils/exportCsv';

function StateTab() {
  const list = usePagedList(stockService.state, { alertOnly: false });
  return (
    <div className="panel">
      <Toolbar>
        <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} />
        <FilterSelect label="Type" allLabel="Tous les types" value={list.filters.type} options={toOptions(ARTICLE_TYPES)}
                      onChange={(v) => list.updateFilter('type', v)} />
        <label className="check" style={{ height: 40 }}>
          <input type="checkbox" checked={list.filters.alertOnly === true}
                 onChange={(e) => list.updateFilter('alertOnly', e.target.checked)} />
          Sous le seuil d'alerte
        </label>
      </Toolbar>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rowKey="articleId" rows={list.data.content}
                   empty={<EmptyState icon={Boxes} title="Aucun article"
                                      text={list.filters.alertOnly ? 'Aucun article sous son seuil.' : 'Créez des articles dans le catalogue.'} />}
                   columns={[
                     { key: 'nom', header: 'Article', render: (s) => <><strong>{s.nom}</strong><div className="muted" style={{ fontSize: 12 }}>{s.reference}</div></> },
                     { key: 'type', header: 'Type', render: (s) => ARTICLE_TYPES[s.type] },
                     {
                       key: 'dispo', header: 'Disponible', align: 'right',
                       render: (s) => (
                         <span>
                           <strong>{formatQty(s.disponible, s.unite)}</strong>
                           {s.alerte ? <span className="badge badge-warn" style={{ marginLeft: 8 }}>Stock faible</span> : null}
                         </span>
                       ),
                     },
                     { key: 'bloque', header: 'Bloqué', align: 'right', render: (s) => (Number(s.bloque) ? formatQty(s.bloque, s.unite) : '—') },
                     { key: 'clients', header: 'Huile des clients', align: 'right', render: (s) => (Number(s.clients) ? formatQty(s.clients, s.unite) : '—') },
                     { key: 'seuil', header: 'Seuil', align: 'right', render: (s) => (s.seuilMin != null ? formatQty(s.seuilMin, s.unite) : '—') },
                     { key: 'lots', header: 'Lots en stock', render: (s) => (s.lots.length ? s.lots.map((l) => l.numero).join(', ') : '—') },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
    </div>
  );
}

function MovementsTab({ canAdjust, reloadKey }) {
  const [dates, setDates] = useState({ from: '', to: '' });
  const list = usePagedList((p) => stockService.movements({ ...p, ...dayRange(p.fromDate, p.toDate), fromDate: undefined, toDate: undefined }),
    { reloadKey }, 30);
  const setDate = (k, v) => { setDates((d) => ({ ...d, [k]: v })); list.updateFilter(k === 'from' ? 'fromDate' : 'toDate', v); };

  return (
    <div className="panel">
      <Toolbar>
        <FilterSelect label="Type" allLabel="Tous les mouvements" value={list.filters.type} options={toOptions(MOVEMENT_TYPES)}
                      onChange={(v) => list.updateFilter('type', v)} />
        <DateInput label="Du" value={dates.from} onChange={(v) => setDate('from', v)} />
        <DateInput label="Au" value={dates.to} onChange={(v) => setDate('to', v)} />
        <Button variant="secondary" size="sm" onClick={async () => {
          const p = await stockService.movements({ ...list.filters, ...dayRange(list.filters.fromDate, list.filters.toDate),
            fromDate: undefined, toDate: undefined, page: 0, size: 100 });
          exportCsv('mouvements-stock', [
            { header: 'Date', value: (m) => formatDateTime(m.date) }, { header: 'Article', value: (m) => m.articleNom },
            { header: 'Lot', value: (m) => m.lotNumero }, { header: 'Type', value: (m) => MOVEMENT_TYPES[m.type] },
            { header: 'Quantité', value: (m) => Number(m.quantite) * m.sens }, { header: 'Unité', value: (m) => m.unite },
            { header: 'Motif', value: (m) => m.motif }, { header: 'Par', value: (m) => m.username },
          ], p.content);
        }}>Exporter</Button>
      </Toolbar>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content}
                   empty={<EmptyState icon={Boxes} title="Aucun mouvement"
                                      text={canAdjust ? 'Commencez par saisir le stock initial de chaque article.' : 'Aucun mouvement enregistré.'} />}
                   columns={[
                     { key: 'date', header: 'Date', render: (m) => formatDateTime(m.date) },
                     { key: 'article', header: 'Article', render: (m) => <strong>{m.articleNom}</strong> },
                     { key: 'lot', header: 'Lot', render: (m) => m.lotNumero || '—' },
                     { key: 'type', header: 'Type', render: (m) => MOVEMENT_TYPES[m.type] },
                     {
                       key: 'q', header: 'Quantité', align: 'right',
                       render: (m) => <strong style={{ color: m.sens > 0 ? 'var(--success)' : 'var(--danger)' }}>
                         {m.sens > 0 ? '+' : '−'}{formatQty(m.quantite, m.unite)}</strong>,
                     },
                     { key: 'motif', header: 'Motif / document', render: (m) => m.motif || (m.documentType ? `${m.documentType} n° ${m.documentId}` : '—') },
                     { key: 'user', header: 'Par', render: (m) => <span className="muted">{m.username}</span> },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
    </div>
  );
}

function LotsTab({ canAdjust, reloadKey }) {
  const list = usePagedList(stockService.lots, { reloadKey });
  const [blocking, setBlocking] = useState(null);
  const [tracing, setTracing] = useState(null);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');

  const changeStatus = async (lot, statut, reason) => {
    try {
      await stockService.setLotStatus(lot.id, statut, reason);
      setBlocking(null);
      setMotif('');
      list.reload();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="panel">
      <Toolbar>
        <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="Numéro de lot ou article" />
        <FilterSelect label="Propriétaire" allLabel="Tous les propriétaires" value={list.filters.owner} options={toOptions(LOT_OWNERS)}
                      onChange={(v) => list.updateFilter('owner', v)} />
        <FilterSelect label="Statut" allLabel="Tous les statuts" value={list.filters.statut} options={toOptions(LOT_STATUS)}
                      onChange={(v) => list.updateFilter('statut', v)} />
      </Toolbar>
      <Alert type="error">{error || list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} onRowClick={(l) => setTracing(l.id)}
                   empty={<EmptyState icon={Boxes} title="Aucun lot" text="Les lots servent à suivre l'huile en vrac et l'huile des clients." />}
                   columns={[
                     { key: 'numero', header: 'Lot', render: (l) => <strong>{l.numero}</strong> },
                     { key: 'article', header: 'Article', render: (l) => l.articleNom },
                     {
                       key: 'owner', header: 'Propriétaire',
                       render: (l) => (l.proprietaire === 'CLIENT'
                         ? <span className="badge badge-client">Client : {l.clientNom}</span>
                         : <span className="badge badge-role">Huilerie</span>),
                     },
                     { key: 'q', header: 'Quantité', align: 'right', render: (l) => formatQty(l.quantite) },
                     {
                       key: 'statut', header: 'Statut',
                       render: (l) => (l.statut === 'BLOQUE'
                         ? <span className="badge badge-off" title={l.motifBlocage}>Bloqué</span>
                         : <span className="badge badge-ok">Disponible</span>),
                     },
                     ...(canAdjust ? [{
                       key: 'a', header: '', align: 'right',
                       render: (l) => <span onClick={(e) => e.stopPropagation()}>{(l.statut === 'BLOQUE'
                         ? <Button size="sm" variant="secondary" onClick={() => changeStatus(l, 'DISPONIBLE')}><LockOpen size={14} />Débloquer</Button>
                         : <Button size="sm" variant="danger" onClick={() => setBlocking(l)}><Lock size={14} />Bloquer</Button>)}</span>,
                     }] : []),
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {tracing ? <TraceModal lotId={tracing} onClose={() => setTracing(null)} /> : null}
      {blocking ? (
        <Modal title={`Bloquer le lot ${blocking.numero}`} onClose={() => setBlocking(null)}
               footer={(
                 <>
                   <Button variant="secondary" onClick={() => setBlocking(null)}>Annuler</Button>
                   <Button variant="danger" disabled={!motif.trim()} onClick={() => changeStatus(blocking, 'BLOQUE', motif.trim())}>Bloquer le lot</Button>
                 </>
               )}>
          <p style={{ marginBottom: 12 }}>Un lot bloqué n'est plus proposé à la vente.</p>
          <div className="field">
            <label>Motif *</label>
            <input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} placeholder="Ex. analyse d'acidité en attente" />
          </div>
        </Modal>
      ) : null}
    </div>
  );
}

export default function StockPage() {
  const { can } = useAuth();
  const canAdjust = can('STOCK_ADJUST');
  const [tab, setTab] = useState('state');
  const [modal, setModal] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [message, setMessage] = useState('');

  const done = (msg) => {
    setModal(null);
    setMessage(msg);
    setReloadKey((k) => k + 1);
  };

  return (
    <>
      <PageHeader title="Stock" description="Quantités calculées à partir des mouvements enregistrés. Aucune quantité n'est modifiable directement."
                  actions={canAdjust ? (
                    <>
                      <Button variant="secondary" onClick={() => setModal('lot')}><Plus size={18} />Nouveau lot</Button>
                      <Button onClick={() => setModal('mvt')}><Plus size={18} />Nouveau mouvement</Button>
                    </>
                  ) : null} />
      <Alert type="success">{message}</Alert>
      <Tabs value={tab} onChange={setTab}
            tabs={[{ value: 'state', label: 'État du stock' }, { value: 'mvts', label: 'Mouvements' }, { value: 'lots', label: 'Lots' }, { value: 'inv', label: 'Inventaires' }]} />
      {tab === 'state' ? <StateTab key={reloadKey} /> : null}
      {tab === 'mvts' ? <MovementsTab canAdjust={canAdjust} reloadKey={reloadKey} /> : null}
      {tab === 'lots' ? <LotsTab canAdjust={canAdjust} reloadKey={reloadKey} /> : null}
      {tab === 'inv' ? <InventoriesTab canAdjust={canAdjust} reloadKey={reloadKey} /> : null}
      {modal === 'mvt' ? <MovementModal onClose={() => setModal(null)} onDone={() => done('Mouvement enregistré.')} /> : null}
      {modal === 'lot' ? <LotModal onClose={() => setModal(null)} onDone={() => done('Lot créé.')} /> : null}
    </>
  );
}
