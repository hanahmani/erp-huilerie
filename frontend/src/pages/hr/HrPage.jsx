import { useCallback, useEffect, useState } from 'react';
import { HandCoins, Pencil, Play, Plus, Square, Trash2, UserRound, Clock, Wallet } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { hrService } from '../../services/salesDocService';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import EmptyState from '../../components/common/EmptyState';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import EntityFormModal from '../../components/forms/EntityFormModal';
import { formatDateTime, formatDT, fromLocalInput, toLocalInput as toLocalValue } from '../../utils/format';
import { EMPLOYEE_PAYMENT_TYPES, PAYMENT_MODES, toOptions } from '../../utils/labels';
import { normalizeDecimal, toMillimes } from '../../utils/money';

// ---------------------------------------------------------------- outils d'affichage

const pad = (n) => String(n).padStart(2, '0');
const time = (iso) => (iso ? new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(iso)) : '—');
const day = (d) => new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(`${d}T12:00:00`));

/** Durée lisible : 485 → « 8 h 05 ». */
function duration(minutes) {
  if (minutes == null) return '—';
  return `${Math.floor(minutes / 60)} h ${pad(minutes % 60)}`;
}

/** ISO → valeur d'un champ datetime-local ; vide si la date est absente. */
const toLocalInput = (iso) => (iso ? toLocalValue(iso) : '');

/** Reste dû : positif = à payer à l'employé, négatif = avance à récupérer. */
function Reste({ value }) {
  const n = Number(value);
  if (n < 0) return <span className="badge badge-warn" title="Avance non encore couverte par le travail">Avance {formatDT(-n)}</span>;
  return n > 0 ? <strong>{formatDT(n)}</strong> : <span className="muted">{formatDT(0)}</span>;
}

/** Minute courante, pour le compteur des journées en cours. */
function useNow() {
  const [now, setNow] = useState(Date.now());
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 30000); return () => clearInterval(t); }, []);
  return now;
}

/** Pointage du jour : en cours (compteur), terminé (heures) ou rien. */
function Today({ e, now }) {
  const w = e.aujourdhui;
  if (!w) return <span className="muted">—</span>;
  if (e.enCours) {
    const minutes = Math.max(0, Math.floor((now - new Date(w.heureDebut).getTime()) / 60000));
    return <span className="badge badge-ok"><Clock size={12} /> Depuis {time(w.heureDebut)} · {duration(minutes)}</span>;
  }
  return <span>{time(w.heureDebut)} – {time(w.heureFin)} <span className="muted">({duration(w.minutes)})</span></span>;
}

// ---------------------------------------------------------------- fenêtres

/** Paiement à un employé : salaire, journées ou avance (sortie de caisse). */
function PayModal({ employee, onClose, onDone }) {
  const reste = Number(employee.reste);
  const [form, setForm] = useState({ type: reste > 0 ? 'TJM' : 'AVANCE', mode: 'ESPECES', montant: reste > 0 ? String(reste) : '', reference: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (ev) => setForm({ ...form, [k]: ev.target.value });
  const save = async () => {
    setSaving(true);
    setError('');
    try {
      onDone(await hrService.pay(employee.id, { type: form.type, mode: form.mode, montant: normalizeDecimal(form.montant), reference: form.reference || null }));
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };
  return (
    <Modal title={`Paiement — ${employee.prenom} ${employee.nom}`} onClose={onClose}
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={save} loading={saving} disabled={!toMillimes(form.montant)}>Enregistrer le paiement</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="weigh-summary">
        <div><span>{employee.jours} jour(s) × TJM</span><strong>{formatDT(employee.gagne)}</strong></div>
        <div><span>Déjà payé</span><strong>{formatDT(employee.paye)}</strong></div>
        <div><span>Reste dû</span><strong><Reste value={employee.reste} /></strong></div>
      </div>
      <div className="form-grid">
        <div className="field"><label>Type *</label>
          <select className="select" value={form.type} onChange={set('type')}>
            {Object.entries(EMPLOYEE_PAYMENT_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Mode *</label>
          <select className="select" value={form.mode} onChange={set('mode')}>
            {Object.entries(PAYMENT_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>
        <div className="field"><label>Montant (DT) *</label><input className="input" inputMode="decimal" autoFocus value={form.montant} onChange={set('montant')} /></div>
        <div className="field"><label>Référence / note</label><input className="input" maxLength={120} value={form.reference} onChange={set('reference')} placeholder="Ex. semaine du 3 au 8" /></div>
      </div>
      <span className="hint">Le montant sort de la caisse. Une avance peut dépasser le reste dû : elle sera déduite des prochaines journées.</span>
    </Modal>
  );
}

/** Ajout ou correction manuelle d'une journée de travail. */
function WorkDayModal({ workDay, employees, fixedEmployee, onClose, onDone }) {
  const editing = Boolean(workDay?.id);
  const [form, setForm] = useState({
    employeeId: fixedEmployee?.id || workDay?.employeeId || '',
    debut: toLocalInput(workDay?.heureDebut), fin: toLocalInput(workDay?.heureFin),
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (ev) => setForm({ ...form, [k]: ev.target.value });
  const save = async () => {
    setSaving(true);
    setError('');
    const body = { employeeId: Number(form.employeeId), heureDebut: fromLocalInput(form.debut), heureFin: fromLocalInput(form.fin) };
    try {
      onDone(await (editing ? hrService.updateWorkDay(workDay.id, body) : hrService.addWorkDay(body)));
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };
  return (
    <Modal title={editing ? `Journée du ${day(workDay.jour)} — ${workDay.employe}` : 'Ajouter une journée'} onClose={onClose}
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button>
             <Button onClick={save} loading={saving} disabled={!form.employeeId || !form.debut}>Enregistrer</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="form-grid">
        {!editing && !fixedEmployee ? (
          <div className="field wide"><label>Employé *</label>
            <select className="select" value={form.employeeId} onChange={set('employeeId')}>
              <option value="">Choisir…</option>
              {employees.map((e) => <option key={e.id} value={e.id}>{e.prenom} {e.nom} ({e.matricule})</option>)}
            </select></div>
        ) : null}
        <div className="field"><label>Début *</label><input className="input" type="datetime-local" value={form.debut} onChange={set('debut')} /></div>
        <div className="field"><label>Fin</label><input className="input" type="datetime-local" value={form.fin} onChange={set('fin')} />
          <span className="hint">Vide = journée encore en cours.</span></div>
      </div>
      {editing ? <span className="hint">La journée garde le TJM du jour pointé ({formatDT(workDay.tjm)}).</span>
        : <span className="hint">Pour un jour oublié : la journée compte au TJM actuel de l'employé.</span>}
    </Modal>
  );
}

const employeeFields = [
  { name: 'prenom', label: 'Prénom', required: true }, { name: 'nom', label: 'Nom', required: true },
  { name: 'telephone', label: 'Téléphone' }, { name: 'cin', label: 'CIN' },
  { name: 'tjm', label: 'TJM — taux journalier (DT)', type: 'number', required: true, hint: 'Montant gagné pour chaque journée pointée.' },
  { name: 'adresse', label: 'Adresse', wide: true },
];

// ---------------------------------------------------------------- journées et paiements (tableaux réutilisés)

function WorkDaysTable({ rows, canManage, onEdit, onDelete, showEmployee }) {
  return (
    <DataTable rows={rows} empty={<EmptyState icon={Clock} title="Aucune journée" text="Les journées pointées avec Play / Stop apparaissent ici." />}
               columns={[
                 ...(showEmployee ? [{ key: 'e', header: 'Employé', render: (w) => <><strong>{w.employe}</strong><div className="muted" style={{ fontSize: 12 }}>{w.matricule}</div></> }] : []),
                 { key: 'j', header: 'Jour', render: (w) => day(w.jour) },
                 { key: 'd', header: 'Début', render: (w) => time(w.heureDebut) },
                 { key: 'f', header: 'Fin', render: (w) => (w.heureFin ? time(w.heureFin) : <span className="badge badge-ok">En cours</span>) },
                 { key: 'h', header: 'Durée', align: 'right', render: (w) => duration(w.minutes) },
                 { key: 't', header: 'TJM', align: 'right', render: (w) => formatDT(w.tjm) },
                 { key: 'u', header: 'Saisi par', render: (w) => <span className="muted">{w.saisiPar}{w.modifiePar ? ` · corrigé par ${w.modifiePar}` : ''}</span> },
                 ...(canManage ? [{
                   key: 'a', header: '', align: 'right',
                   render: (w) => (
                     <div className="actions" onClick={(ev) => ev.stopPropagation()}>
                       <Button variant="secondary" size="sm" onClick={() => onEdit(w)}><Pencil size={14} />Heures</Button>
                       <Button variant="danger" size="sm" onClick={() => onDelete(w)} aria-label="Supprimer la journée"><Trash2 size={14} /></Button>
                     </div>
                   ),
                 }] : []),
               ]} />
  );
}

function PaymentsTable({ rows, showEmployee }) {
  return (
    <DataTable rows={rows} empty={<EmptyState icon={Wallet} title="Aucun paiement" text="Salaires, journées et avances payés apparaissent ici." />}
               columns={[
                 { key: 'n', header: 'N°', render: (p) => <strong>{p.numero}</strong> },
                 { key: 'd', header: 'Date', render: (p) => formatDateTime(p.date) },
                 ...(showEmployee ? [{ key: 'e', header: 'Employé', render: (p) => p.employe }] : []),
                 { key: 't', header: 'Type', render: (p) => <span className={`badge ${p.type === 'AVANCE' ? 'badge-warn' : ''}`}>{EMPLOYEE_PAYMENT_TYPES[p.type]}</span> },
                 { key: 'm', header: 'Mode', render: (p) => PAYMENT_MODES[p.mode] },
                 { key: 'r', header: 'Référence', render: (p) => p.reference || '—' },
                 { key: 'v', header: 'Montant', align: 'right', render: (p) => <strong>{formatDT(p.montant)}</strong> },
                 { key: 'u', header: 'Par', render: (p) => <span className="muted">{p.username}</span> },
               ]} />
  );
}

/** Fiche d'un employé : compte, journées et paiements. */
function EmployeeDetail({ employee, canManage, canPay, onClose, onChanged }) {
  const [e, setE] = useState(employee);
  const [days, setDays] = useState(null);
  const [pays, setPays] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [paying, setPaying] = useState(false);

  const load = useCallback(async () => {
    try {
      const [emp, d, p] = await Promise.all([hrService.employee(employee.id), hrService.workDays({ employeeId: employee.id, size: 100 }),
        hrService.payments({ employeeId: employee.id, size: 100 })]);
      setE(emp); setDays(d.content); setPays(p.content);
    } catch (err) { setError(err.message); }
  }, [employee.id]);
  useEffect(() => { load(); }, [load]);
  const changed = () => { load(); onChanged(); };

  if (paying) return <PayModal employee={e} onClose={() => setPaying(false)} onDone={() => { setPaying(false); changed(); }} />;
  if (editing) return <WorkDayModal workDay={editing.id ? editing : null} fixedEmployee={editing.id ? null : e} onClose={() => setEditing(null)} onDone={() => { setEditing(null); changed(); }} />;

  return (
    <Modal title={`${e.prenom} ${e.nom} · ${e.matricule}`} onClose={onClose} wide
           footer={(<>
             <Button variant="secondary" onClick={onClose}>Fermer</Button>
             {canManage ? <Button variant="secondary" onClick={() => setEditing({})}><Plus size={16} />Ajouter une journée</Button> : null}
             {canPay ? <Button onClick={() => setPaying(true)}><HandCoins size={16} />Payer</Button> : null}
           </>)}>
      <Alert type="error">{error}</Alert>
      <div className="weigh-summary">
        <div><span>TJM</span><strong>{formatDT(e.tjm)}</strong></div>
        <div><span>Journées</span><strong>{e.jours}</strong></div>
        <div><span>Gagné</span><strong>{formatDT(e.gagne)}</strong></div>
        <div><span>Payé</span><strong>{formatDT(e.paye)}</strong></div>
        <div><span>Reste dû</span><strong><Reste value={e.reste} /></strong></div>
      </div>
      <h2 style={{ margin: '16px 0 8px' }}>Journées</h2>
      {!days ? <Spinner /> : <WorkDaysTable rows={days} canManage={canManage} onEdit={setEditing} onDelete={setDeleting} />}
      <h2 style={{ margin: '16px 0 8px' }}>Paiements</h2>
      {!pays ? <Spinner /> : <PaymentsTable rows={pays} />}
      {deleting ? (
        <ConfirmDialog title="Supprimer la journée" danger confirmLabel="Supprimer"
                       message={`La journée du ${day(deleting.jour)} (${formatDT(deleting.tjm)}) ne comptera plus dans le reste dû.`}
                       onCancel={() => setDeleting(null)}
                       onConfirm={async () => { try { await hrService.deleteWorkDay(deleting.id); setDeleting(null); changed(); } catch (err) { setError(err.message); setDeleting(null); } }} />
      ) : null}
    </Modal>
  );
}

// ---------------------------------------------------------------- onglets

function EmployeesTab({ canManage, canPay }) {
  const list = usePagedList(hrService.employees, { actif: 'true' }, 50);
  const now = useNow();
  const [form, setForm] = useState(null);
  const [open, setOpen] = useState(null);
  const [paying, setPaying] = useState(null);
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState('');

  const act = async (id, fn) => {
    setBusy(id);
    setError('');
    try { await fn(); list.reload(); } catch (err) { setError(err.message); } finally { setBusy(null); }
  };

  return (
    <div className="panel">
      <Toolbar>
        <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="Nom, matricule, téléphone, CIN" />
        <FilterSelect label="Statut" allLabel="Tous" value={list.filters.actif}
                      options={[{ value: 'true', label: 'Actifs' }, { value: 'false', label: 'Inactifs' }]}
                      onChange={(v) => list.updateFilter('actif', v)} />
        {canManage ? <Button onClick={() => setForm({})}><Plus size={18} />Nouvel employé</Button> : null}
      </Toolbar>
      <Alert type="error">{error || list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} onRowClick={(e) => setOpen(e)}
                   empty={<EmptyState icon={UserRound} title="Aucun employé" text="Ajoutez vos employés avec leur TJM, puis pointez leurs journées avec Play / Stop." />}
                   columns={[
                     { key: 'n', header: 'Employé', render: (e) => <><strong>{e.prenom} {e.nom}</strong><div className="muted" style={{ fontSize: 12 }}>{e.matricule}{e.telephone ? ` · ${e.telephone}` : ''}</div></> },
                     { key: 't', header: 'TJM', align: 'right', render: (e) => formatDT(e.tjm) },
                     { key: 'p', header: "Aujourd'hui", render: (e) => <Today e={e} now={now} /> },
                     {
                       key: 'c', header: 'Pointage',
                       render: (e) => (!canManage || !e.actif ? null : (
                         <div className="actions" onClick={(ev) => ev.stopPropagation()}>
                           {e.enCours
                             ? <Button variant="stop" size="sm" loading={busy === e.id} onClick={() => act(e.id, () => hrService.stop(e.id))}><Square size={14} fill="currentColor" />Stop</Button>
                             : !e.aujourdhui
                               ? <Button variant="play" size="sm" loading={busy === e.id} onClick={() => act(e.id, () => hrService.start(e.id))}><Play size={14} fill="currentColor" />Play</Button>
                               : null}
                         </div>
                       )),
                     },
                     { key: 'j', header: 'Jours', align: 'right', render: (e) => e.jours },
                     { key: 'g', header: 'Gagné', align: 'right', render: (e) => formatDT(e.gagne) },
                     { key: 'y', header: 'Payé', align: 'right', render: (e) => formatDT(e.paye) },
                     { key: 'r', header: 'Reste dû', align: 'right', render: (e) => <Reste value={e.reste} /> },
                     { key: 's', header: 'Statut', render: (e) => <StatusBadge active={e.actif} /> },
                     ...(canManage || canPay ? [{
                       key: 'a', header: '', align: 'right',
                       render: (e) => (
                         <div className="actions" onClick={(ev) => ev.stopPropagation()}>
                           {canPay ? <Button variant="secondary" size="sm" onClick={() => setPaying(e)}><HandCoins size={14} />Payer</Button> : null}
                           {canManage ? <Button variant="secondary" size="sm" onClick={() => setForm(e)} aria-label="Modifier"><Pencil size={14} /></Button> : null}
                           {canManage ? <Button variant="ghost" size="sm" onClick={() => act(e.id, () => hrService.setStatus(e.id, !e.actif))}>{e.actif ? 'Désactiver' : 'Réactiver'}</Button> : null}
                         </div>
                       ),
                     }] : []),
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {form ? (
        <EntityFormModal title={form.id ? `${form.prenom} ${form.nom} (${form.matricule})` : 'Nouvel employé'} fields={employeeFields}
                         initial={form.id ? { ...form, tjm: String(form.tjm) } : {}} onClose={() => setForm(null)}
                         onSubmit={async (body) => { await hrService.saveEmployee(form.id, body); setForm(null); list.reload(); }} />
      ) : null}
      {paying ? <PayModal employee={paying} onClose={() => setPaying(null)} onDone={() => { setPaying(null); list.reload(); }} /> : null}
      {open ? <EmployeeDetail employee={open} canManage={canManage} canPay={canPay} onClose={() => setOpen(null)} onChanged={list.reload} /> : null}
    </div>
  );
}

function useEmployeeOptions() {
  const [employees, setEmployees] = useState([]);
  useEffect(() => { hrService.employees({ size: 100 }).then((p) => setEmployees(p.content)).catch(() => {}); }, []);
  return employees;
}

function WorkDaysTab({ canManage }) {
  const list = usePagedList(hrService.workDays, {});
  const employees = useEmployeeOptions();
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [error, setError] = useState('');
  return (
    <div className="panel">
      <Toolbar>
        <FilterSelect label="Employé" allLabel="Tous les employés" value={list.filters.employeeId}
                      options={employees.map((e) => ({ value: String(e.id), label: `${e.prenom} ${e.nom}` }))}
                      onChange={(v) => list.updateFilter('employeeId', v)} />
        <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
        <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
        {canManage ? <Button onClick={() => setEditing({})}><Plus size={18} />Ajouter une journée</Button> : null}
      </Toolbar>
      <Alert type="error">{error || list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div>
        : <WorkDaysTable rows={list.data.content} canManage={canManage} onEdit={setEditing} onDelete={setDeleting} showEmployee />}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {editing ? <WorkDayModal workDay={editing.id ? editing : null} employees={employees.filter((e) => e.actif)} onClose={() => setEditing(null)}
                               onDone={() => { setEditing(null); list.reload(); }} /> : null}
      {deleting ? (
        <ConfirmDialog title="Supprimer la journée" danger confirmLabel="Supprimer"
                       message={`La journée du ${day(deleting.jour)} de ${deleting.employe} ne comptera plus dans son reste dû.`}
                       onCancel={() => setDeleting(null)}
                       onConfirm={async () => { try { await hrService.deleteWorkDay(deleting.id); setDeleting(null); list.reload(); } catch (err) { setError(err.message); setDeleting(null); } }} />
      ) : null}
    </div>
  );
}

function PaymentsTab() {
  const list = usePagedList(hrService.payments, {});
  const employees = useEmployeeOptions();
  return (
    <div className="panel">
      <Toolbar>
        <FilterSelect label="Employé" allLabel="Tous les employés" value={list.filters.employeeId}
                      options={employees.map((e) => ({ value: String(e.id), label: `${e.prenom} ${e.nom}` }))}
                      onChange={(v) => list.updateFilter('employeeId', v)} />
        <FilterSelect label="Type" allLabel="Tous les types" value={list.filters.type} options={toOptions(EMPLOYEE_PAYMENT_TYPES)}
                      onChange={(v) => list.updateFilter('type', v)} />
        <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
        <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
      </Toolbar>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : <PaymentsTable rows={list.data.content} showEmployee />}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
    </div>
  );
}

export default function HrPage() {
  const { can } = useAuth();
  const canManage = can('HR_MANAGE');
  const canPay = can(['HR_MANAGE', 'FINANCE_MANAGE']);
  const [tab, setTab] = useState('employees');
  return (
    <>
      <PageHeader title="Ressources humaines"
                  description="Employés, pointage Play / Stop et paiements. Reste dû = journées pointées × TJM − salaires, journées et avances payés." />
      <Tabs value={tab} onChange={setTab}
            tabs={[{ value: 'employees', label: 'Employés' }, { value: 'days', label: 'Pointages' }, { value: 'payments', label: 'Paiements' }]} />
      {tab === 'employees' ? <EmployeesTab canManage={canManage} canPay={canPay} /> : null}
      {tab === 'days' ? <WorkDaysTab canManage={canManage} /> : null}
      {tab === 'payments' ? <PaymentsTab /> : null}
    </>
  );
}
