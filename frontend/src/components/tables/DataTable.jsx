import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';

/**
 * Tableau générique. columns : [{ key, header, render?, align?, sortable? }]
 * Tri : sort = { key, dir: 'asc' | 'desc' } et onSort(key) ; seules les colonnes sortable sont cliquables.
 */
export default function DataTable({ columns, rows, rowKey = 'id', empty, onRowClick, sort, onSort }) {
  if (!rows.length) return empty ?? null;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            {columns.map((c) => {
              const cls = c.align === 'right' ? 'num' : undefined;
              if (!c.sortable || !onSort) return <th key={c.key} className={cls}>{c.header}</th>;
              const active = sort?.key === c.key;
              const Icon = !active ? ArrowUpDown : sort.dir === 'desc' ? ArrowDown : ArrowUp;
              return (
                <th key={c.key} className={cls}
                    aria-sort={active ? (sort.dir === 'desc' ? 'descending' : 'ascending') : 'none'}>
                  <button type="button" className={`th-sort${active ? ' active' : ''}`} onClick={() => onSort(c.key)}>
                    {c.header}<Icon size={14} />
                  </button>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row[rowKey]} className={onRowClick ? 'clickable' : undefined}
                onClick={onRowClick ? () => onRowClick(row) : undefined}>
              {columns.map((c) => (
                <td key={c.key} className={c.align === 'right' ? 'num' : undefined}>
                  {c.render ? c.render(row) : row[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
