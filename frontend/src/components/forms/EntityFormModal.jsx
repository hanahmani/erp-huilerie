import { useState } from 'react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Alert from '../common/Alert';
import FormFields, { toPayload } from './FormFields';

/** Fenêtre de création / modification générique. */
export default function EntityFormModal({ title, fields, initial, submitLabel = 'Enregistrer', onSubmit, onClose }) {
  const [values, setValues] = useState(() => {
    const v = {};
    fields.forEach((f) => { v[f.name] = initial?.[f.name] ?? (f.type === 'checkbox' ? false : ''); });
    return v;
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const change = (name, value) => setValues((v) => ({ ...v, [name]: value }));

  const submit = async () => {
    const missing = {};
    fields.forEach((f) => {
      if (f.required && !f.hidden?.(values) && (values[f.name] === '' || values[f.name] === null)) {
        missing[f.name] = 'Champ obligatoire.';
      }
    });
    if (Object.keys(missing).length) {
      setErrors(missing);
      return;
    }
    setSaving(true);
    setError('');
    setErrors({});
    try {
      await onSubmit(toPayload(fields, values));
    } catch (err) {
      setError(err.message);
      setErrors(err.fieldErrors || {});
      setSaving(false);
    }
  };

  return (
    <Modal title={title} onClose={onClose} wide
           footer={(
             <>
               <Button variant="secondary" onClick={onClose}>Annuler</Button>
               <Button onClick={submit} loading={saving}>{submitLabel}</Button>
             </>
           )}>
      <Alert type="error">{error}</Alert>
      <FormFields fields={fields} values={values} errors={errors} onChange={change} />
    </Modal>
  );
}
