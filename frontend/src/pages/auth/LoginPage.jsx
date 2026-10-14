import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import TextField from '../../components/forms/TextField';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const u = await login(form.username, form.password);
      const target = u.mustChangePassword ? '/profil' : location.state?.from || '/';
      navigate(target, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} noValidate>
      <h1>Connexion</h1>
      <p>Entrez l'identifiant et le mot de passe fournis par l'administrateur.</p>
      {location.state?.idle ? <Alert type="info">Vous avez été déconnecté après une période d'inactivité.</Alert> : null}
      <Alert type="error">{error}</Alert>
      <TextField label="Identifiant" name="username" autoComplete="username" autoFocus required
                 value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
      <TextField label="Mot de passe" name="password" type="password" autoComplete="current-password" required
                 value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
      <Button type="submit" block loading={loading} disabled={!form.username || !form.password}>
        Se connecter
      </Button>
    </form>
  );
}
