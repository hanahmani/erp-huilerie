import { useCallback, useState } from 'react';

const newKey = () => (crypto.randomUUID ? crypto.randomUUID()
  : `${Date.now()}-${Math.random().toString(16).slice(2)}`);

/** Une clé par vente : renvoyée telle quelle si on reclique, renouvelée après succès. */
export function useIdempotencyKey() {
  const [key, setKey] = useState(newKey);
  const renew = useCallback(() => setKey(newKey()), []);
  return [key, renew];
}
