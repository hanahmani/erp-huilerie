import { useCallback, useEffect, useState } from 'react';
import { KeyRound, Pencil, Trash2, UserPlus, Users } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import Modal from '../../components/common/Modal';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import DataTable from '../../components/tables/DataTable';
import { SearchBox, Toolbar, FilterSelect } from '../../components/tables/Toolbar';
import TextField from '../../components/forms/TextField';
import CreateUserModal from './CreateUserModal';
import { formatDateTime } from '../../utils/format';

function EditUserModal({ user, roles, canAssignAdmin, onClose, onSaved }) {
  const [form, setForm] = useState({
    firstName: user.firstName, lastName: user.lastName, email: user.email || '', telephone: user.telephone || '',
    service: user.service || '', roles: user.roles,
  });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const toggle = (code) => setForm({ ...form, roles: form.roles.includes(code) ? form.roles.filter((r) => r !== code) : [...form.roles, code] });
  const submit = async () => {
    setSaving(true);
    setError('');
    try { onSaved(await userService.update(user.id, { ...form, email: form.email || null, telephone: form.telephone || null, service: form.service || null })); } catch (e) { setError(e.message); setSaving(false); }
  };
  return (
    <Modal title={`Modifier ${user.username}`} onClose={onClose}
           footer={(<><Button variant="secondary" onClick={onClose}>Annuler</Button><Button loading={saving} disabled={!form.roles.length} onClick={submit}>Enregistrer</Button></>)}>
      <Alert type="error">{error}</Alert>
      <div className="row">
        <TextField label="Prénom" value={form.firstName} onChange={set('firstName')} />
        <TextField label="Nom" value={form.lastName} onChange={set('lastName')} />
        <TextField label="E-mail" type="email" value={form.email} onChange={set('email')} />
        <TextField label="Téléphone" value={form.telephone} onChange={set('telephone')} />
      </div>
      <TextField label="Service" value={form.service} onChange={set('service')} hint="Ex. Production, Caisse, Magasin" />
      <div className="field"><label>Rôles</label>
        <div className="checks">{roles.filter((r) => canAssignAdmin || r.code !== 'ADMIN').map((r) => (
          <label key={r.code} className="check"><input type="checkbox" checked={form.roles.includes(r.code)} onChange={() => toggle(r.code)} />{r.name}</label>
        ))}</div>
        <span className="hint">Changer les rôles déconnecte l'utilisateur de ses sessions en cours.</span>
      </div>
    </Modal>
  );
}

export default function UsersPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [modal, setModal] = useState(null);   // { type, user }
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setError('');
    try {
      const [u, r] = await Promise.all([userService.list(), userService.roles()]);
      setUsers(u);
      setRoles(r);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const roleName = (code) => roles.find((r) => r.code === code)?.name ?? code;
  const replace = (u) => setUsers((list) => list.map((x) => (x.id === u.id ? u : x)));
  const run = async (fn, message) => {
    setBusy(true);
    setError('');
    try { await fn(); setSuccess(message); setModal(null); setPassword(''); } catch (e) { setError(e.message); setModal(null); } finally { setBusy(false); }
  };

  const term = q.trim().toLowerCase();
  const rows = users.filter((u) => (!term || `${u.firstName} ${u.lastName} ${u.username} ${u.email ?? ''} ${u.service ?? ''}`.toLowerCase().includes(term))
    && (!roleFilter || u.roles.includes(roleFilter))
    && (!statusFilter || String(u.active) === statusFilter));

  const columns = [
    { key: 'name', header: 'Nom', render: (u) => <><strong>{u.firstName} {u.lastName}</strong><div className="muted" style={{ fontSize: 12 }}>{u.email || u.telephone || ''}</div></> },
    { key: 'username', header: 'Identifiant' },
    { key: 'service', header: 'Service', render: (u) => u.service || '—' },
    { key: 'roles', header: 'Rôles', render: (u) => u.roles.map((r) => <span key={r} className="badge badge-role">{roleName(r)}</span>) },
    { key: 'status', header: 'Statut', render: (u) => (u.active ? <span className="badge badge-ok">{u.mustChangePassword ? 'Actif, mot de passe provisoire' : 'Actif'}</span> : <span className="badge badge-off">Désactivé</span>) },
    { key: 'last', header: 'Dernière connexion', render: (u) => (u.derniereConnexion ? formatDateTime(u.derniereConnexion) : <span className="muted">Jamais</span>) },
    {
      key: 'actions', header: '', align: 'right',
      render: (u) => (u.username === me.username ? null : (
        <div className="actions">
          <Button size="sm" variant="secondary" onClick={() => setModal({ type: 'edit', user: u })} aria-label="Modifier"><Pencil size={14} /></Button>
          <Button size="sm" variant="secondary" onClick={() => setModal({ type: 'reset', user: u })} aria-label="Réinitialiser le mot de passe"><KeyRound size={14} /></Button>
          {!u.derniereConnexion ? <Button size="sm" variant="danger" onClick={() => setModal({ type: 'delete', user: u })} aria-label="Supprimer"><Trash2 size={14} /></Button> : null}
          <Button size="sm" variant={u.active ? 'danger' : 'secondary'} onClick={() => setModal({ type: 'toggle', user: u })}>{u.active ? 'Désactiver' : 'Réactiver'}</Button>
        </div>
      )),
    },
  ];

  const m = modal;
  return (
    <>
      <PageHeader title="Utilisateurs" description="Chaque employé a son propre compte. Ses rôles décident de ce qu'il peut voir et faire."
                  actions={<Button onClick={() => setModal({ type: 'create' })}><UserPlus size={18} />Nouvel utilisateur</Button>} />
      <Alert type="error">{error}</Alert>
      <Alert type="success">{success}</Alert>
      <div className="panel">
        <Toolbar>
          <SearchBox value={q} onChange={setQ} />
          <FilterSelect label="Rôle" allLabel="Tous les rôles" value={roleFilter} options={roles.map((r) => ({ value: r.code, label: r.name }))} onChange={setRoleFilter} />
          <FilterSelect label="Statut" allLabel="Tous" value={statusFilter} options={[{ value: 'true', label: 'Actifs' }, { value: 'false', label: 'Désactivés' }]} onChange={setStatusFilter} />
        </Toolbar>
        {loading ? <div className="panel-body"><Spinner /></div> : (
          <DataTable columns={columns} rows={rows} empty={<EmptyState icon={Users} title="Aucun utilisateur" text="Aucun compte ne correspond." />} />
        )}
      </div>

      {m?.type === 'create' ? (
        <CreateUserModal roles={roles} canAssignAdmin={me.roles.includes('ADMIN')} onClose={() => setModal(null)}
                         onCreated={(u) => { setUsers((l) => [...l, u]); setModal(null); setSuccess(`Compte « ${u.username} » créé. Communiquez-lui son mot de passe provisoire.`); }} />
      ) : null}
      {m?.type === 'edit' ? (
        <EditUserModal user={m.user} roles={roles} canAssignAdmin={me.roles.includes('ADMIN')} onClose={() => setModal(null)}
                       onSaved={(u) => { replace(u); setModal(null); setSuccess('Utilisateur modifié.'); }} />
      ) : null}
      {m?.type === 'reset' ? (
        <Modal title={`Réinitialiser le mot de passe de ${m.user.username}`} onClose={() => setModal(null)}
               footer={(<><Button variant="secondary" onClick={() => setModal(null)}>Annuler</Button>
                 <Button loading={busy} disabled={password.length < 10}
                         onClick={() => run(async () => replace(await userService.resetPassword(m.user.id, password)), 'Mot de passe réinitialisé. L\'utilisateur devra le changer à sa prochaine connexion.')}>Réinitialiser</Button></>)}>
          <TextField label="Nouveau mot de passe provisoire" type="password" autoComplete="new-password" value={password}
                     onChange={(e) => setPassword(e.target.value)} hint="10 caractères minimum, avec une lettre et un chiffre. Ses sessions seront coupées." />
        </Modal>
      ) : null}
      {m?.type === 'delete' ? (
        <ConfirmDialog title={`Supprimer ${m.user.username} ?`} message="Ce compte n'a jamais été utilisé : il sera supprimé définitivement." danger
                       confirmLabel="Supprimer" loading={busy} onCancel={() => setModal(null)}
                       onConfirm={() => run(async () => { await userService.remove(m.user.id); setUsers((l) => l.filter((x) => x.id !== m.user.id)); }, 'Compte supprimé.')} />
      ) : null}
      {m?.type === 'toggle' ? (
        <ConfirmDialog title={m.user.active ? 'Désactiver ce compte ?' : 'Réactiver ce compte ?'}
                       message={m.user.active ? `${m.user.firstName} ${m.user.lastName} sera déconnecté immédiatement.` : `${m.user.firstName} ${m.user.lastName} pourra de nouveau se connecter.`}
                       confirmLabel={m.user.active ? 'Désactiver' : 'Réactiver'} danger={m.user.active} loading={busy} onCancel={() => setModal(null)}
                       onConfirm={() => run(async () => replace(await userService.setActive(m.user.id, !m.user.active)), m.user.active ? 'Compte désactivé.' : 'Compte réactivé.')} />
      ) : null}
    </>
  );
}
