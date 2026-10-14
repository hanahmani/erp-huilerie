export default function Button({ variant = 'primary', size, block, loading, children, className = '', ...props }) {
  const classes = ['btn', `btn-${variant}`, size === 'sm' && 'btn-sm', block && 'btn-block', className]
    .filter(Boolean).join(' ');
  return (
    <button type="button" className={classes} disabled={loading || props.disabled} {...props}>
      {loading ? <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : null}
      {children}
    </button>
  );
}
