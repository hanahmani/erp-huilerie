import { useEffect, useRef, useState } from 'react';

/**
 * Déconnexion automatique après une période d'inactivité (minutes définies dans les paramètres,
 * 30 par défaut). Un avertissement est affiché une minute avant.
 */
export function useInactivityLogout(minutes, onLogout) {
  const [warning, setWarning] = useState(false);
  const last = useRef(Date.now());

  useEffect(() => {
    const limit = (minutes || 30) * 60000;
    const bump = () => { last.current = Date.now(); setWarning(false); };
    const events = ['mousedown', 'keydown', 'touchstart', 'scroll'];
    events.forEach((e) => window.addEventListener(e, bump, { passive: true }));
    const timer = setInterval(() => {
      const idle = Date.now() - last.current;
      if (idle >= limit) onLogout();
      else setWarning(idle >= limit - 60000);
    }, 10000);
    return () => { events.forEach((e) => window.removeEventListener(e, bump)); clearInterval(timer); };
  }, [minutes, onLogout]);

  return warning;
}
