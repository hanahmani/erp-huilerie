import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import Modal from '../common/Modal';
import { searchService } from '../../services/salesDocService';

/** Recherche globale (Ctrl+K) : clients, fournisseurs, articles, lots, documents, employés. */
export default function GlobalSearch() {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const [hits, setHits] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const input = useRef(null);

  useEffect(() => {
    const onKey = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setOpen(true); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);
  useEffect(() => { if (open) setTimeout(() => input.current?.focus(), 0); }, [open]);
  useEffect(() => {
    if (q.trim().length < 2) { setHits([]); return undefined; }
    setLoading(true);
    const t = setTimeout(() => searchService.search(q.trim()).then(setHits).catch(() => setHits([])).finally(() => setLoading(false)), 250);
    return () => clearTimeout(t);
  }, [q]);

  const groups = hits.reduce((acc, h) => ({ ...acc, [h.type]: [...(acc[h.type] || []), h] }), {});
  return (
    <>
      <button type="button" className="search-trigger" onClick={() => setOpen(true)}>
        <Search size={16} /><span>Rechercher…</span><kbd>Ctrl K</kbd>
      </button>
      {open ? (
        <Modal title="Recherche" onClose={() => { setOpen(false); setQ(''); }} wide>
          <label className="search-box" style={{ maxWidth: 'none', marginBottom: 14 }}>
            <Search size={16} />
            <input ref={input} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Code, nom, référence, numéro de document ou de lot…" />
          </label>
          {loading ? <p className="muted">Recherche…</p> : null}
          {!loading && q.trim().length >= 2 && !hits.length ? <p className="muted">Aucun résultat.</p> : null}
          {Object.entries(groups).map(([type, list]) => (
            <div key={type} style={{ marginBottom: 12 }}>
              <div className="nav-group-title" style={{ color: 'var(--muted)', opacity: 1, padding: '0 0 4px' }}>{type}</div>
              {list.map((h, i) => (
                <button key={i} type="button" className="search-hit" onClick={() => { setOpen(false); setQ(''); navigate(h.lien); }}>
                  <strong>{h.titre}</strong><span className="muted">{h.detail}</span>
                </button>
              ))}
            </div>
          ))}
        </Modal>
      ) : null}
    </>
  );
}
