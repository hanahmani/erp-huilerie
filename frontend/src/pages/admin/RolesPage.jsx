import { useCallback, useEffect, useState } from 'react';
import { Plus, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import PageHeader from '../../components/common/PageHeader';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import TextField from '../../components/forms/TextField';

const MODULES = [
  ['USER', 'Utilisateurs'], ['ROLE', 'Rôles'], ['AUDIT', 'Journal'], ['SETTINGS', 'Paramètres'], ['DASHBOARD', 'Tableau de bord'],
  ['CLIENT', 'Clients'], ['SUPPLIER', 'Fournisseurs'], ['CATALOG', 'Catalogue'], ['STOCK', 'Stock'], ['PURCHASE', 'Achats'],
  ['SALES', 'Ventes'], ['CASH', 'Caisse'], ['FINANCE', 'Finance'], ['REPORT', 'Rapports'], ['PRODUCTION', 'Production'],
  ['PRESSING', 'Pressage'], ['PACKAGING', 'Conditionnement'],
];
const moduleOf = (code) => MODULES.find(([p]) => code.startsWith(`${p}_`))?.[1] ?? 'Autres';

export default function RolesPage() {
  const { can } = useAuth();
  const canManage = can('ROLE_MANAGE');
  const [roles, setRoles] = useState(null);
  const [permissions, setPermissions] = useState([]);
  const [selected, setSelected] = useState(null);     // rôle en édition (id null = nouveau)
  const [draft, setDraft] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    try {
      const [r, p] = await Promise.all([userService.roles(), userService.permissions()]);
      setRoles(r);
      setPermissions(p);
    } catch (e) {
      setError(e.message);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const pick = (role) => {
    setSelected(role);
    setDraft(role ? { code: role.code, name: role.name, description: role.description || '', permissions: [...role.permissions] }
      : { code: '', name: '', description: '', permissions: [] });
    setSuccess('');
    setError('');
  };

  const toggle = (code) => setDraft((d) => ({ ...d, permissions: d.permissions.includes(code) ? d.permissions.filter((c) => c !== code) : [...d.permissions, code] }));
  const toggleModule = (codes, on) => setDraft((d) => ({ ...d, permissions: on ? [...new Set([...d.permissions, ...codes])] : d.permissions.filter((c) => !codes.includes(c)) }));

  const save = async () => {
    setBusy(true);
    setError('');
    try {
      const saved = await userService.saveRole(selected?.id, { ...draft, code: selected ? null : draft.code.trim().toUpperCase(), description: draft.description || null });
      await load();
      pick(saved);
      setSuccess('Rôle enregistré. Les droits s\'appliquent immédiatement aux utilisateurs concernés.');
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    setDeleting(false);
    try {
      await userService.deleteRole(selected.id);
      setSelected(null);
      setDraft(null);
      await load();
      setSuccess('Rôle supprimé.');
    } catch (e) {
      setError(e.message);
    }
  };

  if (!roles) return error ? <Alert type="error">{error}</Alert> : <Spinner />;
  const groups = MODULES.map(([, label]) => [label, permissions.filter((p) => moduleOf(p.code) === label)]).filter(([, list]) => list.length);
  const isAdmin = selected?.code === 'ADMIN';
  const readOnly = !canManage || isAdmin;

  return (
    <>
      <PageHeader title="Rôles et permissions" description="Un rôle regroupe des permissions. Le serveur vérifie chaque permission à chaque requête."
                  actions={canManage ? <Button onClick={() => pick(null)}><Plus size={18} />Nouveau rôle</Button> : null} />
      <Alert type="error">{error}</Alert>
      <Alert type="success">{success}</Alert>
      <div className="roles-layout">
        <div className="panel roles-list">
          {roles.map((r) => (
            <button key={r.code} type="button" className={`roles-item${selected?.code === r.code ? ' active' : ''}`} onClick={() => pick(r)}>
              <strong>{r.name}</strong>
              <span>{r.permissions.length} permission{r.permissions.length > 1 ? 's' : ''}{r.systeme ? ' · système' : ''}</span>
            </button>
          ))}
        </div>
        <div className="panel">
          {!draft ? (
            <div className="empty"><div className="empty-icon"><ShieldCheck size={26} /></div><h2>Choisissez un rôle</h2>
              <p>Sélectionnez un rôle pour voir et modifier ses permissions.</p></div>
          ) : (
            <div className="panel-body">
              {isAdmin ? <Alert type="info">Le rôle Administrateur possède toujours toutes les permissions.</Alert> : null}
              <div className="row">
                <TextField label="Code" value={draft.code} disabled={Boolean(selected)} onChange={(e) => setDraft({ ...draft, code: e.target.value })}
                           hint={selected ? 'Non modifiable.' : 'Majuscules, ex. MAGASINIER'} />
                <TextField label="Nom" value={draft.name} disabled={readOnly} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
              </div>
              <TextField label="Description" value={draft.description} disabled={readOnly} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
              <div className="perm-matrix">
                {groups.map(([label, list]) => {
                  const codes = list.map((p) => p.code);
                  const all = codes.every((c) => draft.permissions.includes(c));
                  return (
                    <div key={label} className="perm-row">
                      <label className="perm-module">
                        <input type="checkbox" disabled={readOnly} checked={all} onChange={(e) => toggleModule(codes, e.target.checked)} />
                        <strong>{label}</strong>
                      </label>
                      <div className="perm-items">
                        {list.map((p) => (
                          <label key={p.code} className="check" title={p.code}>
                            <input type="checkbox" disabled={readOnly} checked={draft.permissions.includes(p.code)} onChange={() => toggle(p.code)} />
                            {p.description}
                          </label>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
              {!readOnly ? (
                <div className="actions" style={{ marginTop: 16 }}>
                  {selected && !selected.systeme ? <Button variant="danger" onClick={() => setDeleting(true)}>Supprimer le rôle</Button> : null}
                  <Button loading={busy} disabled={!draft.name.trim() || (!selected && !draft.code.trim())} onClick={save}>Enregistrer le rôle</Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </div>
      {deleting ? <ConfirmDialog title={`Supprimer le rôle ${selected.name} ?`} message="Possible uniquement s'il n'est attribué à personne."
                                 danger confirmLabel="Supprimer" onConfirm={remove} onCancel={() => setDeleting(false)} /> : null}
    </>
  );
}
