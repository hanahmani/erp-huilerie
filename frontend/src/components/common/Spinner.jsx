export default function Spinner({ fullscreen }) {
  const el = <span className="spinner" role="status" aria-label="Chargement" />;
  return fullscreen ? <div className="center-screen">{el}</div> : el;
}
