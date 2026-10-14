import { useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import TextField from '../../components/forms/TextField';
import { userService } from '../../services/userService';
import { roleLabel } from '../../utils/roles';

const EMPTY = { username: '', firstName: '', lastName: '', temporaryPassword: '', roles: [], email: '', telephone: '', service: '' };

export default function CreateUserModal({ roles, canAssignAdmin, onClose, onCreated }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });
  const toggleRole = (code) => setForm({
    ...form,
    roles: form.roles.includes(code) ? form.roles.filter((r) => r !== code) : [...form.roles, code],
  });

  const submit = async () => {
    setError('');
    setErrors({});
    setLoading(true);
    try {
      const created = await userService.create({ ...form, username: form.username.trim().toLowerCase(), email: form.email || null, telephone: form.telephone || null, service: form.service || null });
      onCreated(created);
    } catch (err) {
      setError(err.message);
      setErrors(err.fieldErrors || {});
    } finally {
      setLoading(false);
    }
  };

  const valid = form.username && form.firstName && form.lastName && form.temporaryPassword && form.roles.length;

  return (
    <Modal
      title="Nouvel utilisateur"
      onClose={onClose}
      footer={(
        <>
          <Button variant="secondary" onClick={onClose}>Annuler</Button>
          <Button onClick={submit} loading={loading} disabled={!valid}>Créer l'utilisateur</Button>
        </>
      )}
    >
      <Alert type="error">{error}</Alert>
      <div className="row">
        <TextField label="Prénom" value={form.firstName} onChange={set('firstName')} error={errors.firstName} />
        <TextField label="Nom" value={form.lastName} onChange={set('lastName')} error={errors.lastName} />
      </div>
      <div className="row">
        <TextField label="E-mail" type="email" value={form.email} onChange={set('email')} error={errors.email} />
        <TextField label="Téléphone" value={form.telephone} onChange={set('telephone')} />
      </div>
      <TextField label="Service" value={form.service} onChange={set('service')} />
      <TextField label="Identifiant de connexion" value={form.username} onChange={set('username')}
                 hint="Minuscules, chiffres, point ou tiret. Exemple : s.benali" error={errors.username} />
      <TextField label="Mot de passe provisoire" type="password" autoComplete="new-password"
                 value={form.temporaryPassword} onChange={set('temporaryPassword')}
                 hint="L'utilisateur devra le changer à sa première connexion." error={errors.temporaryPassword} />
      <div className="field">
        <label>Rôles</label>
        <div className="checks">
          {roles.filter((r) => canAssignAdmin || r.code !== 'ADMIN').map((r) => (
            <label key={r.code} className="check">
              <input type="checkbox" checked={form.roles.includes(r.code)} onChange={() => toggleRole(r.code)} />
              {r.name || roleLabel(r.code)}
            </label>
          ))}
        </div>
        {errors.roles ? <span className="error">{errors.roles}</span> : null}
      </div>
    </Modal>
  );
}
