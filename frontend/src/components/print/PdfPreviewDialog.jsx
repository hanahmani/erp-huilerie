import { useEffect, useState } from 'react';
import { Download, ExternalLink } from 'lucide-react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import Alert from '../common/Alert';
import Spinner from '../common/Spinner';

/**
 * Aperçu d'un PDF généré par le backend, avec téléchargement et ouverture dans un nouvel onglet.
 * load() renvoie { blob, fileName } ; une erreur (paramètre manquant…) est affichée telle que l'API la formule.
 */
export default function PdfPreviewDialog({ title, load, onClose }) {
  const [pdf, setPdf] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let url;
    let cancelled = false;
    load()
      .then(({ blob, fileName }) => {
        if (cancelled) return;
        url = URL.createObjectURL(blob);
        setPdf({ url, fileName });
      })
      .catch((e) => { if (!cancelled) setError(e.message); });
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [load]);

  const download = () => {
    const a = document.createElement('a');
    a.href = pdf.url;
    a.download = pdf.fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return (
    <Modal title={title} onClose={onClose} wide
           footer={(
             <>
               <Button variant="secondary" onClick={onClose}>Fermer</Button>
               <Button variant="secondary" disabled={!pdf} onClick={() => window.open(pdf.url, '_blank', 'noopener')}>
                 <ExternalLink size={16} />Nouvel onglet
               </Button>
               <Button disabled={!pdf} onClick={download}><Download size={16} />Télécharger</Button>
             </>
           )}>
      <Alert type="error">{error}</Alert>
      {!pdf && !error ? <Spinner /> : null}
      {pdf ? <iframe className="pdf-frame" src={pdf.url} title={title} /> : null}
    </Modal>
  );
}
