import { useEffect, useState } from 'react';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import FormFields, { toPayload } from '../../components/forms/FormFields';
import { settingsService } from '../../services/settingsService';
import { catalogService } from '../../services/catalogService';

const baseFields = [
  { name: 'raisonSociale', label: 'Raison sociale', required: true, wide: true, hint: 'Imprimée en tête des tickets.' },
  { name: 'adresse', label: 'Adresse', wide: true },
  { name: 'telephone', label: 'Téléphone' },
  { name: 'email', label: 'E-mail', type: 'email' },
  { name: 'matriculeFiscal', label: 'Matricule fiscal' },
  { name: 'messageTicket', label: 'Message en bas du ticket', hint: 'Ex. Merci de votre visite.' },
  {
    name: 'venteCreditAutorisee', label: 'Autoriser les ventes à crédit', type: 'checkbox', wide: true,
    hint: 'Permet un paiement partiel pour un client identifié, dans la limite de son plafond.',
  },
];

export default function CompanySettingsForm() {
  const [values, setValues] = useState(null);
  const [oils, setOils] = useState([]);
  useEffect(() => {
    catalogService.articles({ actif: true, size: 100 }).then((p) => setOils(p.content.filter((a) => a.suiviLot))).catch(() => {});
  }, []);
  const fields = [
    ...baseFields,
    { name: 'tarifPressageKg', label: 'Tarif du pressage (DT par kg d\'olives)', type: 'number', hint: 'Prérempli à chaque réception, modifiable.' },
    { name: 'pressageArticleId', label: 'Article « huile des clients »', type: 'select', numeric: true, placeholder: 'Aucun',
      options: oils.map((a) => ({ value: a.id, label: `${a.nom} (${a.uniteCode})` })), hint: 'Article suivi par lot dans lequel est stockée l\'huile des clients.' },
  ];
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    settingsService.get()
      .then((s) => setValues({ ...s, raisonSociale: s.raisonSociale ?? '', tarifPressageKg: s.tarifPressageKg ?? '', pressageArticleId: s.pressageArticleId ?? '' }))
      .catch((e) => setError(e.message));
  }, []);

  if (!values) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setErrors({});
    setSaved(false);
    try {
      const s = await settingsService.save(toPayload(fields, values));
      setValues({ ...s, tarifPressageKg: s.tarifPressageKg ?? '', pressageArticleId: s.pressageArticleId ?? '' });
      setSaved(true);
    } catch (err) {
      setError(err.message);
      setErrors(err.fieldErrors || {});
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="panel" onSubmit={submit} style={{ maxWidth: 760 }}>
      <div className="panel-body">
        {!values.configured ? (
          <Alert type="info">Les informations de l'huilerie ne sont pas encore saisies. Elles apparaîtront sur les tickets.</Alert>
        ) : null}
        <Alert type="error">{error}</Alert>
        {saved ? <Alert type="success">Paramètres enregistrés.</Alert> : null}
        <FormFields fields={fields} values={values} errors={errors}
                    onChange={(n, v) => setValues((s) => ({ ...s, [n]: v }))} />
        <p className="hint muted" style={{ marginBottom: 16, fontSize: 13 }}>
          Logo des tickets : placez le fichier <code>frontend/public/logo/logo.png</code> puis reconstruisez le frontend.
        </p>
        <Button type="submit" loading={saving}>Enregistrer les paramètres</Button>
      </div>
    </form>
  );
}
