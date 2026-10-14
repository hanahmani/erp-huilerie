// Emblème de l'huilerie : un rameau et une olive dorée.
export default function OliveMark({ size = 40 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <rect width="64" height="64" rx="14" fill="#2f3a1b" />
      <path d="M14 46c10-4 22-14 34-30" stroke="#eef1df" strokeWidth="3" fill="none" strokeLinecap="round" />
      <ellipse cx="25" cy="30" rx="9" ry="4.5" transform="rotate(-40 25 30)" fill="#8e9c55" />
      <ellipse cx="39" cy="34" rx="8" ry="4" transform="rotate(20 39 34)" fill="#8e9c55" />
      <ellipse cx="44" cy="20" rx="5.5" ry="7" fill="#b8892a" />
    </svg>
  );
}
