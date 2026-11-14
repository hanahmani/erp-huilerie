// Client HTTP unique : ajoute le jeton, transforme les erreurs de l'API en messages lisibles.
const BASE_URL = import.meta.env.VITE_API_URL || import.meta.env.BASE_URL + 'api';
const TOKEN_KEY = 'huilerie.token';

// « Se souvenir de moi » : localStorage (survit à la fermeture du navigateur), sinon sessionStorage.
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY),
  remembered: () => Boolean(localStorage.getItem(TOKEN_KEY)),
  set: (token, remember) => {
    tokenStore.clear();
    (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token);
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
  },
};

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request(method, path, body) {
  const headers = { Accept: 'application/json' };
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.');
  }

  if (response.status === 401 && token) {
    tokenStore.clear();
    window.dispatchEvent(new Event('auth:expired'));
  }
  if (response.status === 204) return null;

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.message || `Erreur ${response.status}`, data?.fieldErrors || {});
  }
  return data;
}

/** Téléchargement binaire (PDF…) avec le jeton ; en cas d'erreur, le message JSON de l'API est remonté. */
async function requestBlob(path) {
  const headers = { Accept: 'application/pdf, application/json' };
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { headers });
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.');
  }
  if (response.status === 401 && token) {
    tokenStore.clear();
    window.dispatchEvent(new Event('auth:expired'));
  }
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new ApiError(response.status, data?.message || `Erreur ${response.status}`);
  }
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename="?([^";]+)"?/);
  return { blob: await response.blob(), fileName: match ? match[1] : 'document.pdf' };
}

/** Envoi d'un fichier (multipart) ; le navigateur fixe lui-même le Content-Type. */
async function upload(method, path, field, file) {
  const headers = { Accept: 'application/json' };
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  const body = new FormData();
  body.append(field, file);
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { method, headers, body });
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.');
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status, data?.message || `Erreur ${response.status}`, data?.fieldErrors || {});
  return data;
}

export const API_BASE_URL = BASE_URL;

export const api = {
  blob: requestBlob,
  upload,
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body ?? {}),
  patch: (path, body) => request('PATCH', path, body ?? {}),
  put: (path, body) => request('PUT', path, body ?? {}),
  delete: (path) => request('DELETE', path),
};

/** Construit ?a=1&b=2 en ignorant les valeurs vides. */
export function toQuery(params = {}) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.append(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}
