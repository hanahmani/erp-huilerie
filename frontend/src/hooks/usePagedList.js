import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Charge une liste paginée côté serveur.
 * fetcher(params) doit renvoyer { content, page, totalPages, totalElements }.
 */
export function usePagedList(fetcher, initialFilters = {}, size = 20) {
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const fetchRef = useRef(fetcher);
  fetchRef.current = fetcher;

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await fetchRef.current({ ...filters, page, size }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [filters, page, size]);

  useEffect(() => {
    const t = setTimeout(reload, 200);  // petite attente pendant la frappe
    return () => clearTimeout(t);
  }, [reload]);

  const updateFilter = (key, value) => {
    setPage(0);
    setFilters((f) => ({ ...f, [key]: value }));
  };

  return { data, loading, error, filters, updateFilter, setFilters, page, setPage, reload };
}
