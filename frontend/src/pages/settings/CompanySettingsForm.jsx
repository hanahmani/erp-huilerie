import { useEffect, useRef, useState } from 'react';
import { ImageUp, Trash2 } from 'lucide-react';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import FormFields, { toPayload } from '../../components/forms/FormFields';
import { settingsService } from '../../services/settingsService';
import { catalogService } from '../../services/catalogService';

const baseFields = [
  { name: 'raisonSociale', label: 'Raison sociale', required: true, wide: true, hint: 'Imprimée en tête des tickets, devis et factures.' },
  { name: 'adresse', label: 'Adresse', wide: true, hint: 'Obligatoire pour générer les devis et factures en PDF.' },
  { name: 'telephone', label: 'Téléphone' },
  { name: 'email', label: 'E-mail', type: 'email' },
  { name: 'matriculeFiscal', label: 'Matricule fiscal', hint: 'Obligatoire pour générer les devis et factures en PDF.' },
  { name: 'rne', label: 'Identifiant RNE', hint: 'Registre national des entreprises.' },
  { name: 'mentionsFacture', label: 'Mentions des factures', type: 'textarea', wide: true,
    hint: 'Conditions de paiement, pénalités… Une mention par ligne. Les conditions de paiement du client sont ajoutées automatiquement.' },
  { name: 'mentionsDevis', label: 'Mentions des devis', type: 'textarea', wide: true, hint: 'Validité de l\'offre, délais… Une mention par ligne.' },
  { name: 'mentionExport', label: 'Mention export', wide: true, hint: 'Imprimée sur les documents export, ex. « Exonéré de TVA – export ».' },
  { name: 'mentionEchantillon', label: 'Mention échantillons', wide: true, hint: 'Imprimée sur les envois d\'échantillons, ex. « Échantillons sans valeur commerciale ».' },
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
    { name: 'timbreFiscal', label: 'Timbre fiscal par facture (DT)', type: 'number', hint: 'Ajouté à chaque facture validée. Vide = aucun.' },
    { name: 'inactiviteMinutes', label: 'Déconnexion après inactivité (minutes)', type: 'number', hint: '30 minutes par défaut.' },
    { name: 'densiteHuile', label: 'Densité de l\'huile (kg par litre)', type: 'number',
      hint: '1 L = 0,916 kg par défaut. Sert à vendre au litre l\'huile stockée en kg.' },
    { name: 'tarifPressageKg', label: 'Tarif du pressage (DT par kg d\'olives)', type: 'number', hint: 'Prérempli à chaque réception, modifiable.' },
    { name: 'pressageArticleId', label: 'Article « huile des clients »', type: 'select', numeric: true, placeholder: 'Aucun',
      options: oils.map((a) => ({ value: a.id, label: `${a.nom} (${a.uniteCode})` })), hint: 'Article suivi par lot dans lequel est stockée l\'huile des clients.' },
  ];
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [logoVersion, setLogoVersion] = useState(() => Date.now());
  const [logoBusy, setLogoBusy] = useState(false);
  const fileInput = useRef(null);

  useEffect(() => {
    settingsService.get()
      .then((s) => setValues({ ...s, raisonSociale: s.raisonSociale ?? '', tarifPressageKg: s.tarifPressageKg ?? '', pressageArticleId: s.pressageArticleId ?? '', timbreFiscal: s.timbreFiscal ?? '', inactiviteMinutes: s.inactiviteMinutes ?? '' }))
      .catch((e) => setError(e.message));
  }, []);

  if (!values) return error ? <Alert type="error">{error}</Alert> : <Spinner />;

  const changeLogo = async (fn) => {
    setLogoBusy(true);
    setError('');
    try {
      const s = await fn();
      setValues((v) => ({ ...v, logo: s.logo }));
      setLogoVersion(Date.now());
    } catch (err) {
      setError(err.message);
    } finally {
      setLogoBusy(false);
      if (fileInput.current) fileInput.current.value = '';
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setErrors({});
    setSaved(false);
    try {
      const s = await settingsService.save(toPayload(fields, values));
      setValues({ ...s, tarifPressageKg: s.tarifPressageKg ?? '', pressageArticleId: s.pressageArticleId ?? '', timbreFiscal: s.timbreFiscal ?? '', inactiviteMinutes: s.inactiviteMinutes ?? '' });
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
        <div className="field">
          <label>Logo</label>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            {values.logo ? <img src={settingsService.logoUrl(logoVersion)} alt="Logo de l'huilerie" style={{ maxHeight: 64, maxWidth: 200, border: '1px solid var(--beige-300)', borderRadius: 6, padding: 4 }} /> : <span className="muted">Aucun logo téléversé.</span>}
            <input ref={fileInput} type="file" accept="image/png,image/jpeg" hidden
                   onChange={(e) => e.target.files[0] && changeLogo(() => settingsService.uploadLogo(e.target.files[0]))} />
            <Button type="button" variant="secondary" loading={logoBusy} disabled={!values.configured} onClick={() => fileInput.current?.click()}>
              <ImageUp size={16} />{values.logo ? 'Remplacer' : 'Téléverser'}
            </Button>
            {values.logo ? <Button type="button" variant="danger" loading={logoBusy} onClick={() => changeLogo(settingsService.deleteLogo)}><Trash2 size={16} />Supprimer</Button> : null}
          </div>
          <small className="hint muted">PNG ou JPEG, 500 Ko au maximum. Imprimé sur les devis, factures et tickets.
            {values.configured ? '' : ' Enregistrez d\'abord les informations ci-dessus.'} Sans logo téléversé, les tickets utilisent <code>logo/logo.png</code>.</small>
        </div>
        <Button type="submit" loading={saving}>Enregistrer les paramètres</Button>
      </div>
    </form>
  );
}
