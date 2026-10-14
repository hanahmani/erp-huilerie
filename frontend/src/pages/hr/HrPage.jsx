import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, Plus, Trash2, UserRound } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usePagedList } from '../../hooks/usePagedList';
import { hrService } from '../../services/salesDocService';
import { userService } from '../../services/userService';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import DataTable from '../../components/tables/DataTable';
import Pagination from '../../components/tables/Pagination';
import EntityFormModal from '../../components/forms/EntityFormModal';
import { Toolbar, SearchBox, FilterSelect, DateInput } from '../../components/tables/Toolbar';
import { formatDateTime, todayISO } from '../../utils/format';
import { CONTRACT_TYPES, EMPLOYEE_STATUS, LEAVE_STATUS, LEAVE_STATUS_CLASS, LEAVE_TYPES, toOptions } from '../../utils/labels';

const d = (iso) => (iso ? new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR') : '—');

function EmployeesTab({ canManage }) {
  const list = usePagedList(hrService.employees, { statut: 'ACTIF' });
  const [refs, setRefs] = useState({ services: [], positions: [], users: [] });
  const [editing, setEditing] = useState(null);
  useEffect(() => {
    Promise.all([hrService.services(), hrService.positions(), canManage ? userService.list().catch(() => []) : Promise.resolve([])])
      .then(([services, positions, users]) => setRefs({ services, positions, users })).catch(() => {});
  }, [canManage]);
  const fields = [
    { name: 'prenom', label: 'Prénom', required: true }, { name: 'nom', label: 'Nom', required: true },
    { name: 'cin', label: 'CIN' }, { name: 'dateNaissance', label: 'Date de naissance', type: 'date' },
    { name: 'telephone', label: 'Téléphone' }, { name: 'email', label: 'E-mail', type: 'email' },
    { name: 'adresse', label: 'Adresse', wide: true },
    { name: 'serviceId', label: 'Service', type: 'select', numeric: true, placeholder: 'Aucun', options: refs.services.map((s) => ({ value: s.id, label: s.nom })) },
    { name: 'posteId', label: 'Poste', type: 'select', numeric: true, placeholder: 'Aucun', options: refs.positions.map((s) => ({ value: s.id, label: s.nom })) },
    { name: 'dateEmbauche', label: "Date d'embauche", type: 'date', required: true },
    { name: 'typeContrat', label: 'Type de contrat', type: 'select', required: true, placeholder: false, options: toOptions(CONTRACT_TYPES) },
    { name: 'dateFinContrat', label: 'Fin de contrat', type: 'date', hint: 'Obligatoire pour CDD, saisonnier et stage.' },
    { name: 'statut', label: 'Statut', type: 'select', required: true, placeholder: false, options: toOptions(EMPLOYEE_STATUS) },
    { name: 'soldeConges', label: 'Solde de congés (jours)', type: 'number', required: true },
    { name: 'userId', label: 'Compte utilisateur lié', type: 'select', numeric: true, placeholder: 'Aucun', options: refs.users.map((u) => ({ value: u.id, label: `${u.firstName} ${u.lastName} (${u.username})` })),
      hint: 'Permet à l\'employé de demander ses congés lui-même.' },
  ];
  const daysLeft = (iso) => (iso ? Math.ceil((new Date(`${iso}T12:00:00`) - new Date()) / 86400000) : null);
  return (
    <div className="panel">
      <Toolbar>
        <SearchBox value={list.filters.q} onChange={(v) => list.updateFilter('q', v)} placeholder="Nom, matricule, CIN" />
        <FilterSelect label="Service" allLabel="Tous les services" value={list.filters.serviceId} options={refs.services.map((s) => ({ value: s.id, label: s.nom }))} onChange={(v) => list.updateFilter('serviceId', v)} />
        <FilterSelect label="Contrat" allLabel="Tous contrats" value={list.filters.contrat} options={toOptions(CONTRACT_TYPES)} onChange={(v) => list.updateFilter('contrat', v)} />
        <FilterSelect label="Statut" allLabel="Tous statuts" value={list.filters.statut} options={toOptions(EMPLOYEE_STATUS)} onChange={(v) => list.updateFilter('statut', v)} />
        {canManage ? <Button size="sm" onClick={() => setEditing({ typeContrat: 'CDI', statut: 'ACTIF', soldeConges: '0', dateEmbauche: todayISO() })}><Plus size={16} />Nouvel employé</Button> : null}
      </Toolbar>
      <Alert type="error">{list.error}</Alert>
      {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
        <DataTable rows={list.data.content} onRowClick={canManage ? (e) => setEditing(e) : undefined}
                   empty={<EmptyState icon={UserRound} title="Aucun employé" text="Créez les dossiers du personnel, y compris les saisonniers de la campagne." />}
                   columns={[
                     { key: 'm', header: 'Matricule', render: (e) => <span className="muted">{e.matricule}</span> },
                     { key: 'n', header: 'Nom', render: (e) => <strong>{e.prenom} {e.nom}</strong> },
                     { key: 's', header: 'Service / poste', render: (e) => [e.service, e.poste].filter(Boolean).join(' · ') || '—' },
                     { key: 'c', header: 'Contrat', render: (e) => {
                       const left = daysLeft(e.dateFinContrat);
                       return <>{CONTRACT_TYPES[e.typeContrat]}{e.dateFinContrat ? <span className={left != null && left <= 30 ? 'badge badge-warn' : 'muted'} style={{ marginLeft: 6 }}>fin {d(e.dateFinContrat)}</span> : null}</>;
                     } },
                     { key: 'e', header: 'Embauche', render: (e) => d(e.dateEmbauche) },
                     { key: 'l', header: 'Solde congés', align: 'right', render: (e) => `${Number(e.soldeConges)} j` },
                     { key: 'u', header: 'Compte', render: (e) => e.username || '—' },
                     { key: 't', header: 'Statut', render: (e) => <span className={`badge ${e.statut === 'ACTIF' ? 'badge-ok' : 'badge-off'}`}>{EMPLOYEE_STATUS[e.statut]}</span> },
                   ]} />
      )}
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {editing ? (
        <EntityFormModal title={editing.id ? `${editing.prenom} ${editing.nom} (${editing.matricule})` : 'Nouvel employé'} fields={fields}
                         initial={{ ...editing, serviceId: editing.serviceId ?? '', posteId: editing.posteId ?? '', userId: editing.userId ?? '', soldeConges: String(editing.soldeConges ?? '0') }}
                         onClose={() => setEditing(null)} onSubmit={async (p) => { await hrService.saveEmployee(editing.id, p); setEditing(null); list.reload(); }} />
      ) : null}
    </div>
  );
}

function LeavesTab({ canValidate, canManage, isHr }) {
  const list = usePagedList(hrService.leaves, { statut: canValidate ? 'EN_ATTENTE' : '' });
  const [me, setMe] = useState(undefined);
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState(null);
  const [days, setDays] = useState(null);
  const [refusing, setRefusing] = useState(null);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    hrService.me().then(setMe).catch(() => setMe(null));
    if (canManage) hrService.employees({ statut: 'ACTIF', size: 100 }).then((p) => setEmployees(p.content)).catch(() => {});
  }, [canManage]);
  useEffect(() => {
    if (form?.dateDebut && form?.dateFin && form.dateFin >= form.dateDebut) hrService.workingDays(form.dateDebut, form.dateFin).then(setDays).catch(() => setDays(null));
    else setDays(null);
  }, [form?.dateDebut, form?.dateFin]);
  const run = async (fn) => {
    setError('');
    try { await fn(); list.reload(); hrService.me().then(setMe).catch(() => {}); } catch (e) { setError(e.message); }
  };
  return (
    <>
      {me ? <Alert type="info">Votre solde de congé annuel : <strong>{Number(me.soldeConges)} jour(s)</strong>.</Alert> : null}
      {me === null && !isHr ? <Alert type="info">Votre compte n'est lié à aucune fiche employé : demandez aux RH de l'associer.</Alert> : null}
      <Alert type="error">{error}</Alert>
      <div className="panel">
        <Toolbar>
          <FilterSelect label="Statut" allLabel="Toutes les demandes" value={list.filters.statut} options={toOptions(LEAVE_STATUS)} onChange={(v) => list.updateFilter('statut', v)} />
          {me || canManage ? <Button size="sm" onClick={() => setForm({ type: 'CONGE_ANNUEL', dateDebut: todayISO(), dateFin: todayISO(), employeeId: '' })}><Plus size={16} />Demande de congé</Button> : null}
        </Toolbar>
        {list.loading && !list.data.content.length ? <div className="panel-body"><Spinner /></div> : (
          <DataTable rows={list.data.content} empty={<EmptyState icon={CalendarDays} title="Aucune demande" text="Les demandes de congé et leur suivi apparaissent ici." />}
                     columns={[
                       { key: 'e', header: 'Employé', render: (l) => <><strong>{l.employe}</strong><div className="muted" style={{ fontSize: 12 }}>{l.matricule}</div></> },
                       { key: 't', header: 'Type', render: (l) => LEAVE_TYPES[l.type] },
                       { key: 'p', header: 'Période', render: (l) => `${d(l.dateDebut)} → ${d(l.dateFin)}` },
                       { key: 'j', header: 'Jours ouvrés', align: 'right', render: (l) => Number(l.jours) },
                       { key: 'm', header: 'Motif', render: (l) => l.motif || '—' },
                       { key: 's', header: 'Statut', render: (l) => <span className={`badge ${LEAVE_STATUS_CLASS[l.statut]}`} title={l.motifRefus || ''}>{LEAVE_STATUS[l.statut]}</span> },
                       { key: 'x', header: '', align: 'right', render: (l) => (l.statut !== 'EN_ATTENTE' ? <span className="muted" style={{ fontSize: 12 }}>{l.decidePar ? `${l.decidePar}, ${formatDateTime(l.decideLe)}` : ''}</span> : (
                         <div className="actions">
                           {canValidate ? <Button size="sm" onClick={() => run(() => hrService.decide(l.id, true))}>Valider</Button> : null}
                           {canValidate ? <Button size="sm" variant="danger" onClick={() => { setRefusing(l); setMotif(''); }}>Refuser</Button> : null}
                           <Button size="sm" variant="secondary" onClick={() => run(() => hrService.cancelLeave(l.id))}>Annuler</Button>
                         </div>
                       )) },
                     ]} />
        )}
        <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      </div>
      {form ? (
        <Modal title="Demande de congé" onClose={() => setForm(null)}
               footer={(<><Button variant="secondary" onClick={() => setForm(null)}>Annuler</Button>
                 <Button disabled={!days || (canManage && !me && !form.employeeId)} onClick={() => run(async () => {
                   await hrService.requestLeave({ ...form, employeeId: form.employeeId ? Number(form.employeeId) : null, motif: form.motif || null });
                   setForm(null);
                 })}>Envoyer la demande</Button></>)}>
          {canManage ? (
            <div className="field"><label>Employé</label>
              <select className="select" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
                <option value="">{me ? 'Moi-même' : 'Choisir…'}</option>
                {employees.map((e) => <option key={e.id} value={e.id}>{e.prenom} {e.nom} ({e.matricule}) · solde {Number(e.soldeConges)} j</option>)}
              </select></div>
          ) : null}
          <div className="field"><label>Type</label><select className="select" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>{Object.entries(LEAVE_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></div>
          <div className="row">
            <div className="field"><label>Du</label><input type="date" className="input" value={form.dateDebut} onChange={(e) => setForm({ ...form, dateDebut: e.target.value })} /></div>
            <div className="field"><label>Au (inclus)</label><input type="date" className="input" value={form.dateFin} onChange={(e) => setForm({ ...form, dateFin: e.target.value })} /></div>
          </div>
          <p className="hint muted" style={{ marginBottom: 12 }}>{days != null ? `${Number(days)} jour(s) ouvré(s), hors week-ends et jours fériés.` : 'Choisissez une période valide.'}</p>
          <div className="field"><label>Motif</label><input className="input" value={form.motif || ''} onChange={(e) => setForm({ ...form, motif: e.target.value })} /></div>
        </Modal>
      ) : null}
      {refusing ? (
        <Modal title="Refuser la demande" onClose={() => setRefusing(null)}
               footer={(<><Button variant="secondary" onClick={() => setRefusing(null)}>Annuler</Button>
                 <Button variant="danger" disabled={!motif.trim()} onClick={() => run(async () => { await hrService.decide(refusing.id, false, motif.trim()); setRefusing(null); })}>Refuser</Button></>)}>
          <div className="field"><label>Motif du refus *</label><input className="input" value={motif} onChange={(e) => setMotif(e.target.value)} autoFocus /></div>
        </Modal>
      ) : null}
    </>
  );
}

function AbsencesTab({ canManage }) {
  const list = usePagedList(hrService.absences, {});
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState(null);
  useEffect(() => { hrService.employees({ statut: 'ACTIF', size: 100 }).then((p) => setEmployees(p.content)).catch(() => {}); }, []);
  const fields = [
    { name: 'employeeId', label: 'Employé', type: 'select', numeric: true, required: true, options: employees.map((e) => ({ value: e.id, label: `${e.prenom} ${e.nom}` })) },
    { name: 'jour', label: 'Jour', type: 'date', required: true },
    { name: 'duree', label: 'Durée', type: 'select', required: true, placeholder: false, options: [{ value: '1', label: 'Journée' }, { value: '0.5', label: 'Demi-journée' }] },
    { name: 'motif', label: 'Motif', wide: true },
    { name: 'justifiee', label: 'Absence justifiée', type: 'checkbox' },
  ];
  return (
    <div className="panel">
      <Toolbar>
        <DateInput label="Du" value={list.filters.from} onChange={(v) => list.updateFilter('from', v)} />
        <DateInput label="Au" value={list.filters.to} onChange={(v) => list.updateFilter('to', v)} />
        {canManage ? <Button size="sm" onClick={() => setForm({ jour: todayISO(), duree: '1' })}><Plus size={16} />Saisir une absence</Button> : null}
      </Toolbar>
      <DataTable rows={list.data.content} empty={<EmptyState icon={CalendarDays} title="Aucune absence" text="Les absences saisies par les RH apparaissent ici." />}
                 columns={[
                   { key: 'e', header: 'Employé', render: (a) => <strong>{a.employe}</strong> },
                   { key: 'j', header: 'Jour', render: (a) => d(a.jour) },
                   { key: 'd', header: 'Durée', render: (a) => (Number(a.duree) === 1 ? 'Journée' : 'Demi-journée') },
                   { key: 'm', header: 'Motif', render: (a) => a.motif || '—' },
                   { key: 's', header: 'Justifiée', render: (a) => <span className={`badge ${a.justifiee ? 'badge-ok' : 'badge-warn'}`}>{a.justifiee ? 'Oui' : 'Non'}</span> },
                   { key: 'u', header: 'Saisie par', render: (a) => <span className="muted">{a.saisiPar}</span> },
                 ]} />
      <Pagination page={list.page} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onChange={list.setPage} />
      {form ? <EntityFormModal title="Nouvelle absence" fields={fields} initial={form} onClose={() => setForm(null)}
                               onSubmit={async (p) => { await hrService.recordAbsence(p); setForm(null); list.reload(); }} /> : null}
    </div>
  );
}

function RefsTab({ canManage }) {
  const [services, setServices] = useState(null);
  const [positions, setPositions] = useState(null);
  const [holidays, setHolidays] = useState([]);
  const [names, setNames] = useState({ service: '', poste: '', jour: '', libelle: '' });
  const [error, setError] = useState('');
  const load = useCallback(() => Promise.all([hrService.services(), hrService.positions(), hrService.holidays()])
    .then(([s, p, h]) => { setServices(s); setPositions(p); setHolidays(h); }).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);
  const run = async (fn) => { setError(''); try { await fn(); setNames({ service: '', poste: '', jour: '', libelle: '' }); load(); } catch (e) { setError(e.message); } };
  if (!services) return <Spinner />;
  const Block = ({ title, rows, k, add }) => (
    <div className="panel"><div className="section-title"><h2>{title}</h2></div>
      <div className="panel-body">
        {rows.length ? rows.map((r) => <span key={r.id} className="badge badge-role" style={{ marginBottom: 6 }}>{r.nom}</span>) : <p className="muted">Aucun.</p>}
        {canManage ? (
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <input className="input" placeholder="Nouveau…" value={names[k]} onChange={(e) => setNames({ ...names, [k]: e.target.value })} />
            <Button size="sm" disabled={!names[k].trim()} onClick={() => run(() => add(names[k].trim()))}><Plus size={14} />Ajouter</Button>
          </div>
        ) : null}
      </div></div>
  );
  return (
    <>
      <Alert type="error">{error}</Alert>
      <div className="grid-2">
        <Block title="Services" rows={services} k="service" add={(n) => hrService.saveService(null, n)} />
        <Block title="Postes" rows={positions} k="poste" add={(n) => hrService.savePosition(null, n)} />
      </div>
      <div className="panel" style={{ marginTop: 16 }}>
        <div className="section-title"><h2>Jours fériés</h2></div>
        <div className="panel-body">
          <p className="muted" style={{ marginBottom: 10 }}>Exclus du calcul des jours de congé. Les fêtes religieuses changent chaque année : saisissez-les à l'avance.</p>
          <table className="table"><tbody>{holidays.map((h) => (
            <tr key={h.id}><td>{d(h.jour)}</td><td>{h.libelle}</td>
              <td className="num">{canManage ? <button type="button" className="btn btn-ghost btn-sm" onClick={() => run(() => hrService.deleteHoliday(h.id))}><Trash2 size={14} /></button> : null}</td></tr>
          ))}</tbody></table>
          {canManage ? (
            <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
              <input type="date" className="input" style={{ width: 180 }} value={names.jour} onChange={(e) => setNames({ ...names, jour: e.target.value })} />
              <input className="input" placeholder="Libellé" value={names.libelle} onChange={(e) => setNames({ ...names, libelle: e.target.value })} />
              <Button size="sm" disabled={!names.jour || !names.libelle.trim()} onClick={() => run(() => hrService.addHoliday({ jour: names.jour, libelle: names.libelle.trim() }))}><Plus size={14} />Ajouter</Button>
            </div>
          ) : null}
        </div>
      </div>
    </>
  );
}

export default function HrPage() {
  const { can } = useAuth();
  const isHr = can('HR_VIEW');
  const canManage = can('HR_MANAGE');
  const canValidate = can('LEAVE_VALIDATE');
  const tabs = [
    isHr && { value: 'employees', label: 'Employés' },
    { value: 'leaves', label: isHr || canValidate ? 'Congés' : 'Mes congés' },
    isHr && { value: 'absences', label: 'Absences' },
    isHr && { value: 'refs', label: 'Services, postes et jours fériés' },
  ].filter(Boolean);
  const [tab, setTab] = useState(tabs[0].value);
  return (
    <>
      <PageHeader title={isHr ? 'Ressources humaines' : 'Mes congés'}
                  description={isHr ? 'Dossiers du personnel, contrats (y compris saisonniers), congés et absences. Accès réservé aux personnes autorisées.'
                    : 'Demandez un congé et suivez la réponse de votre responsable.'} />
      {tabs.length > 1 ? <Tabs value={tab} onChange={setTab} tabs={tabs} /> : null}
      {tab === 'employees' ? <EmployeesTab canManage={canManage} /> : null}
      {tab === 'leaves' ? <LeavesTab canValidate={canValidate} canManage={canManage} isHr={isHr} /> : null}
      {tab === 'absences' ? <AbsencesTab canManage={canManage} /> : null}
      {tab === 'refs' ? <RefsTab canManage={canManage} /> : null}
    </>
  );
}
