import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import TextField from '../../components/forms/TextField';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';

export default function ChangePasswordPage() {
  const { user, changePassword } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ current: '', next: '', confirm: '' });
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);
  const forced = user.mustChangePassword;

  const mismatch = form.confirm && form.next !== form.confirm;

  const submit = async (e) => {
    e.preventDefault();
    if (mismatch) return;
    setError('');
    setLoading(true);
    try {
      await changePassword(form.current, form.next);
      setDone(true);
      setForm({ current: '', next: '', confirm: '' });
      if (forced) navigate('/', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Changer le mot de passe"
        description={forced
          ? 'Votre mot de passe est provisoire. Choisissez-en un nouveau pour accéder à l\'ERP.'
          : 'Vos autres sessions seront déconnectées après le changement.'}
      />
      <div className="panel" style={{ maxWidth: 480 }}>
        <form className="panel-body" onSubmit={submit} noValidate>
          <Alert type="error">{error}</Alert>
          {done ? <Alert type="success">Mot de passe changé.</Alert> : null}
          <TextField label="Mot de passe actuel" type="password" autoComplete="current-password"
                     value={form.current} onChange={(e) => setForm({ ...form, current: e.target.value })} />
          <TextField label="Nouveau mot de passe" type="password" autoComplete="new-password"
                     hint="10 caractères minimum, avec au moins une lettre et un chiffre."
                     value={form.next} onChange={(e) => setForm({ ...form, next: e.target.value })} />
          <TextField label="Confirmer le nouveau mot de passe" type="password" autoComplete="new-password"
                     error={mismatch ? 'Les deux mots de passe ne correspondent pas.' : ''}
                     value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
          <Button type="submit" loading={loading}
                  disabled={!form.current || !form.next || !form.confirm || mismatch}>
            Changer le mot de passe
          </Button>
        </form>
      </div>
    </>
  );
}
