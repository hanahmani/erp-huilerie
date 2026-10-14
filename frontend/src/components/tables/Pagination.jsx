import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalElements) return null;
  return (
    <div className="pagination">
      <span>{totalElements} résultat{totalElements > 1 ? 's' : ''}</span>
      {totalPages > 1 ? (
        <div className="pagination-nav">
          <button type="button" className="btn btn-secondary btn-sm" disabled={page === 0}
                  onClick={() => onChange(page - 1)} aria-label="Page précédente"><ChevronLeft size={16} /></button>
          <span>Page {page + 1} sur {totalPages}</span>
          <button type="button" className="btn btn-secondary btn-sm" disabled={page + 1 >= totalPages}
                  onClick={() => onChange(page + 1)} aria-label="Page suivante"><ChevronRight size={16} /></button>
        </div>
      ) : null}
    </div>
  );
}
