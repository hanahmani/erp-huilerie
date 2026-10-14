import { useEffect, useState } from 'react';
import { ArrowDown, ArrowUp } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Alert from '../../components/common/Alert';
import Spinner from '../../components/common/Spinner';
import { traceService } from '../../services/operationsService';
import { formatDateTime, formatQty } from '../../utils/format';
import { MOVEMENT_TYPES } from '../../utils/labels';

function Links({ title, icon: Icon, links, onOpen, empty }) {
  return (
    <div className="trace-col">
      <h2><Icon size={18} />{title}</h2>
      {links.length ? links.map((l, i) => (
        <div key={i} className="trace-item">
          <div><strong>{l.type}</strong> · {l.document}</div>
          <div className="muted">{l.date ? formatDateTime(l.date) : ''} · {l.description}{l.quantite != null ? ` : ${formatQty(l.quantite)}` : ''}</div>
          {l.lotId ? <button type="button" className="btn btn-ghost btn-sm" onClick={() => onOpen(l.lotId)}>Voir le lot {l.lotNumero}</button> : null}
        </div>
      )) : <p className="muted">{empty}</p>}
    </div>
  );
}

/** « D'où vient ce lot et où a-t-il été utilisé ou vendu ? » */
export default function TraceModal({ lotId, onClose }) {
  const [id, setId] = useState(lotId);
  const [t, setT] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { setT(null); traceService.lot(id).then(setT).catch((e) => setError(e.message)); }, [id]);

  return (
    <Modal title={t ? `Traçabilité du lot ${t.numero}` : 'Traçabilité'} onClose={onClose} wide>
      <Alert type="error">{error}</Alert>
      {!t ? <Spinner /> : (
        <>
          <p style={{ marginBottom: 14 }}><strong>{t.article}</strong> · {t.proprietaire === 'CLIENT' ? `huile du client ${t.client}` : "propriété de l'huilerie"} · {t.statut === 'BLOQUE' ? 'bloqué' : 'disponible'}</p>
          <div className="grid-2">
            <Links title="Origine (amont)" icon={ArrowUp} links={t.amont} onOpen={setId} empty="Aucune origine enregistrée (stock initial ou mouvement manuel)." />
            <Links title="Utilisation (aval)" icon={ArrowDown} links={t.aval} onOpen={setId} empty="Ce lot n'a pas encore été utilisé ni vendu." />
          </div>
          <h2 style={{ margin: '18px 0 8px' }}>Mouvements du lot</h2>
          <table className="table"><tbody>{t.mouvements.map((m) => (
            <tr key={m.id}><td>{formatDateTime(m.date)}</td><td>{MOVEMENT_TYPES[m.type]}</td>
              <td className="num" style={{ color: m.sens > 0 ? 'var(--success)' : 'var(--danger)' }}>{m.sens > 0 ? '+' : '−'}{formatQty(m.quantite, m.unite)}</td>
              <td className="muted">{m.motif}</td></tr>
          ))}</tbody></table>
        </>
      )}
    </Modal>
  );
}
