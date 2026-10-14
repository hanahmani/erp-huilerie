import { useCallback, useEffect, useState } from 'react';
import { ClipboardCheck, Plus, ShieldAlert } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { qualityService } from '../../services/salesDocService';
import { stockService } from '../../services/stockService';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import RemoteSelect from '../../components/forms/RemoteSelect';
import { Toolbar, SearchBox, FilterSelect } from '../../components/tables/Toolbar';
import SimpleRefTable from '../settings/SimpleRefTable';
import { formatDateTime } from '../../utils/format';
import { ANOMALY_STATUS, GRAVITES, toOptions } from '../../utils/labels';
import { normalizeDecimal } from '../../utils/money';

const range = (c) => [c.valeurMin != null ? `≥ ${Number(c.valeurMin)}` : null, c.valeurMax != null ? `≤ ${Number(c.valeurMax)}` : null].filter(Boolean).join(' et ');

function ControlModal({ onClose, onDone }) {
  const [lotId, setLotId] = useState('');
  const [criteria, setCriteria] = useState([]);
  const [values, setValues] = useState({});
  const [obs, setObs] = useState('');
  const [apply, setApply] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { qualityService.criteria().then((c) => setCriteria(c.filter((x) => x.actif))).catch((e) => setError(e.message)); }, []);
  const fetchLots = useCallback((q) => stockService.lots({ q, size: 30 }), []);
  const toLot = useCallback((l) => ({ value: l.id, label: `${l.numero} · ${l.articleNom}${l.statut === 'BLOQUE' ? ' (bloqué)' : ''}` }), []);
  const filled = criteria.filter((c) => (values[c.id] ?? '') !== '');
  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      onDone(await qualityService.record({ lotId: Number(lotId), observations: obs || null, appliquerDecision: apply,
        resultats: filled.map((c) => ({ criterionId: c.id, valeur: normalizeDecimal(values[c.id]) })) }));
    } catch (e) { setError(e.message); setBusy(false); }
  };
  return (
    <Modal title="Nouveau contrôle qualité" onClose={onClose} wide
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button><Button loading={busy} disabled={!lotId || !filled.length} onClick={submit}>Enregistrer le contrôle</Button></>)}>
      <Alert type="error">{error}</Alert>
      {!criteria.length ? <Alert type="info">Définissez d'abord les critères dans l'onglet Critères (acidité, peroxyde, K232…).</Alert> : null}
      <RemoteSelect label="Lot contrôlé" required value={lotId} onChange={(id) => setLotId(id)} fetcher={fetchLots} toOption={toLot} />
      <table className="table table-edit"><thead><tr><th>Critère</th><th>Seuil</th><th>Résultat</th><th /></tr></thead>
        <tbody>{criteria.map((c) => {
          const v = values[c.id];
          const n = v === undefined || v === '' ? null : Number(normalizeDecimal(v));
          const ok = n === null ? null : (c.valeurMin == null || n >= Number(c.valeurMin)) && (c.valeurMax == null || n <= Number(c.valeurMax));
          return (
            <tr key={c.id}><td><strong>{c.nom}</strong></td><td className="muted">{range(c)} {c.unite || ''}</td>
              <td><input className="input" inputMode="decimal" style={{ width: 130 }} value={v ?? ''} onChange={(e) => setValues({ ...values, [c.id]: e.target.value })} /></td>
              <td>{ok === null ? null : <span className={`badge ${ok ? 'badge-ok' : 'badge-off'}`}>{ok ? 'Conforme' : 'Hors seuil'}</span>}</td></tr>
          );
        })}</tbody></table>
      <div className="field" style={{ marginTop: 12 }}><label>Observations (analyse organoleptique…)</label><textarea className="input textarea" rows={2} value={obs} onChange={(e) => setObs(e.target.value)} /></div>
      <label className="check check-field"><input type="checkbox" checked={apply} onChange={(e) => setApply(e.target.checked)} />
        <span>Appliquer la décision au lot<small>Non conforme : lot bloqué et anomalie ouverte. Conforme : lot débloqué.</small></span></label>
    </Modal>
  );
}

function ControlsTab({ canManage }) {
  const list = usePagedList(qualityService.controls, {});
  const [creating, setCreating] = useState(false);
  const [open, setOpen] = useState(null);
  return (
    <div className="panel">
      <Toolbar>
        <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="N° de contrôle ou de lot" />
        <FilterSelect label="Résultat" allLabel="Tous" value={list.filters.resultat} options={[{ value: 'CONFORME', label: 'Conformes' }, { value: 'NON_CONFORME', label: 'Non conformes' }]} onChange={(v) => list.updateFilter('resultat', v)} />
        {canManage ? <Button size="sm" onClick={() => setCreating(true)}><Plus size={16} />Nouveau contrôle</Button> : null}
      </Toolbar>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} onRowClick={setOpen} empty={<EmptyState icon={ClipboardCheck} title="Aucun contrôle" text="Enregistrez les analyses des lots d'huile ou d'olives." />}
                   columns={[
                     { key: 'n', header: 'N°', render: (c) => <strong>{c.numero}</strong> },
                     { key: 'd', header: 'Date', render: (c) => formatDateTime(c.date) },
                     { key: 'l', header: 'Lot', render: (c) => `${c.lotNumero} · ${c.article}` },
                     { key: 'u', header: 'Contrôleur', render: (c) => c.controleur },
                     { key: 'r', header: 'Résultat', render: (c) => <span className={`badge ${c.resultat === 'CONFORME' ? 'badge-ok' : 'badge-off'}`}>{c.resultat === 'CONFORME' ? 'Conforme' : 'Non conforme'}</span> },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {creating ? <ControlModal onClose={() => setCreating(false)} onDone={() => { setCreating(false); list.reload(); }} /> : null}
      {open ? (
        <Modal title={`Contrôle ${open.numero}`} onClose={() => setOpen(null)}>
          <p style={{ marginBottom: 10 }}>Lot {open.lotNumero} · {open.article} · {formatDateTime(open.date)} · {open.controleur}</p>
          <table className="table"><tbody>{open.resultats.map((r, i) => (
            <tr key={i}><td>{r.critere}</td><td className="num">{Number(r.valeur)} {r.unite || ''}</td><td className="muted">{range({ valeurMin: r.min, valeurMax: r.max })}</td>
              <td><span className={`badge ${r.conforme ? 'badge-ok' : 'badge-off'}`}>{r.conforme ? 'OK' : 'Hors seuil'}</span></td></tr>
          ))}</tbody></table>
          {open.observations ? <p style={{ marginTop: 10 }}>{open.observations}</p> : null}
        </Modal>
      ) : null}
    </div>
  );
}

function AnomaliesTab({ canManage }) {
  const list = usePagedList(qualityService.anomalies, { statut: 'OUVERTE' });
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({});
  const [error, setError] = useState('');
  const fetchLots = useCallback((q) => stockService.lots({ q, size: 30 }), []);
  const toLot = useCallback((l) => ({ value: l.id, label: `${l.numero} · ${l.articleNom}` }), []);
  const save = async () => {
    setError('');
    try {
      if (editing.id) await qualityService.updateAnomaly(editing.id, { actionCorrective: form.action || null, statut: form.statut });
      else await qualityService.createAnomaly({ lotId: form.lotId ? Number(form.lotId) : null, description: form.description, gravite: form.gravite });
      setEditing(null);
      list.reload();
    } catch (e) { setError(e.message); }
  };
  return (
    <div className="panel">
      <Toolbar>
        <FilterSelect label="Statut" allLabel="Tous" value={list.filters.statut} options={toOptions(ANOMALY_STATUS)} onChange={(v) => list.updateFilter('statut', v)} />
        <FilterSelect label="Gravité" allLabel="Toutes gravités" value={list.filters.gravite} options={toOptions(GRAVITES)} onChange={(v) => list.updateFilter('gravite', v)} />
        {canManage ? <Button size="sm" onClick={() => { setEditing({}); setForm({ gravite: 'MINEURE' }); }}><Plus size={16} />Nouvelle anomalie</Button> : null}
      </Toolbar>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} onRowClick={canManage ? (a) => { setEditing(a); setForm({ action: a.actionCorrective || '', statut: a.statut }); } : undefined}
                   empty={<EmptyState icon={ShieldAlert} title="Aucune anomalie" text="Les non-conformités et incidents qualité apparaissent ici." />}
                   columns={[
                     { key: 'n', header: 'N°', render: (a) => <strong>{a.numero}</strong> },
                     { key: 'd', header: 'Date', render: (a) => formatDateTime(a.createdAt) },
                     { key: 'l', header: 'Lot', render: (a) => a.lotNumero || '—' },
                     { key: 'x', header: 'Description', render: (a) => a.description },
                     { key: 'g', header: 'Gravité', render: (a) => <span className={`badge ${a.gravite === 'CRITIQUE' ? 'badge-off' : a.gravite === 'MAJEURE' ? 'badge-warn' : ''}`}>{GRAVITES[a.gravite]}</span> },
                     { key: 's', header: 'Statut', render: (a) => ANOMALY_STATUS[a.statut] },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {editing ? (
        <Modal title={editing.id ? `Anomalie ${editing.numero}` : 'Nouvelle anomalie'} onClose={() => setEditing(null)}
               footer={(<><Button variant="secondary" onClick={() => setEditing(null)}>Annuler</Button><Button onClick={save}>Enregistrer</Button></>)}>
          <Alert type="error">{error}</Alert>
          {editing.id ? (
            <>
              <p style={{ marginBottom: 12 }}>{editing.description}</p>
              <div className="field"><label>Action corrective</label><textarea className="input textarea" rows={3} value={form.action} onChange={(e) => setForm({ ...form, action: e.target.value })} /></div>
              <div className="field"><label>Statut</label><select className="select" value={form.statut} onChange={(e) => setForm({ ...form, statut: e.target.value })}>{Object.entries(ANOMALY_STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
            </>
          ) : (
            <>
              <RemoteSelect label="Lot concerné (facultatif)" value={form.lotId} onChange={(id) => setForm({ ...form, lotId: id })} fetcher={fetchLots} toOption={toLot} />
              <div className="field"><label>Description *</label><textarea className="input textarea" rows={3} value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
              <div className="field"><label>Gravité</label><select className="select" value={form.gravite} onChange={(e) => setForm({ ...form, gravite: e.target.value })}>{Object.entries(GRAVITES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
            </>
          )}
        </Modal>
      ) : null}
    </div>
  );
}

export default function QualityPage() {
  const { can } = useAuth();
  const canManage = can('QUALITY_MANAGE');
  const [tab, setTab] = useState('controls');
  return (
    <>
      <PageHeader title="Contrôle qualité" description="Analyses des lots selon des critères paramétrables, décision de conformité et suivi des anomalies." />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: 'controls', label: 'Contrôles' }, { value: 'anomalies', label: 'Anomalies' }, { value: 'criteria', label: 'Critères' }]} />
      {tab === 'controls' ? <ControlsTab canManage={canManage} /> : null}
      {tab === 'anomalies' ? <AnomaliesTab canManage={canManage} /> : null}
      {tab === 'criteria' ? (
        <SimpleRefTable label="Critères de qualité" createLabel="Nouveau critère" canManage={canManage}
                        emptyText="Exemples : Acidité ≤ 0,8 %, Indice de peroxyde ≤ 20 meq O2/kg, K232 ≤ 2,5, K270 ≤ 0,22."
                        load={qualityService.criteria} save={qualityService.saveCriterion} setStatus={qualityService.criterionStatus}
                        columns={[{ key: 'nom', header: 'Critère', render: (c) => <strong>{c.nom}</strong> },
                                  { key: 's', header: 'Seuil', render: (c) => `${range(c)} ${c.unite || ''}` }]}
                        fields={[{ name: 'nom', label: 'Nom', required: true }, { name: 'unite', label: 'Unité', hint: 'Ex. %, meq O2/kg' },
                                 { name: 'valeurMin', label: 'Valeur minimale', type: 'number' }, { name: 'valeurMax', label: 'Valeur maximale', type: 'number' }]} />
      ) : null}
    </>
  );
}
