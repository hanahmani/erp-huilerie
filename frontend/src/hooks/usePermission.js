import { useAuth } from '../context/AuthContext';

/** Affichage uniquement : le backend reste seul juge des droits. */
export function usePermission(permission) {
  return useAuth().can(permission);
}
