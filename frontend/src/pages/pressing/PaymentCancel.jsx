import { useState } from 'react';
import { Undo2 } from 'lucide-react';
import Button from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import { pressingService } from '../../services/operationsService';
import { formatDT } from '../../utils/format';

/**
 * Annulation d'un paiement du service saisi par erreur : motif obligatoire ; le service redevient dû et une sortie
 * de caisse compense l'encaissement. onDone(detail) reçoit le bulletin mis à jour.
 */
export default function PaymentCancel({ pressingId, numero, montant, onDone }) {
  const { can } = useAuth();
  const [open, setOpen] = useState(false);
  const [motif, setMotif] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (!can(['PRESSING_MANAGE', 'FINANCE_MANAGE'])) return null;

  if (!open) {
    return <Button variant="secondary" onClick={() => setOpen(true)}><Undo2 size={14} />Annuler</Button>;
  }
  const confirm = async () => {
    setBusy(true);
    setError('');
    try {
      const detail = await pressingService.cancelPayment(pressingId, numero, motif.trim());
      setOpen(false);
      onDone?.(detail);
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };
  return (
    <div style={{ marginTop: 6, textAlign: 'left' }}>
      <input className="input" autoFocus maxLength={255} value={motif} placeholder="Motif (ex. saisi par erreur)"
             onChange={(e) => setMotif(e.target.value)} />
      <span className="hint" style={{ display: 'block' }}>{formatDT(montant)} sortiront de la caisse ; le service redevient dû.</span>
      {error ? <span className="error" style={{ display: 'block' }}>{error}</span> : null}
      <div className="actions" style={{ marginTop: 6 }}>
        <Button variant="secondary" onClick={() => { setOpen(false); setMotif(''); }}>Non</Button>
        <Button variant="danger" loading={busy} disabled={!motif.trim()} onClick={confirm}>Annuler le paiement</Button>
      </div>
    </div>
  );
}
