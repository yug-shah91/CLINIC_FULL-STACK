import axios from 'axios';

// Security note: the token lives in localStorage, so any XSS bug could read it. That is an
// accepted tradeoff for this project (see README "Security notes"). The role kept here is
// only a hint for the UI: AuthContext re-checks it with GET /api/auth/me on every load.
export const STORAGE_KEY = 'clinic_user';

// One shared Axios instance. Every request in the app goes through it.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

// REQUEST interceptor: runs before every request leaves the browser.
// Adds "Authorization: Bearer <token>" so the backend's JwtFilter can identify the user.
api.interceptors.request.use((config) => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved?.token) config.headers.Authorization = `Bearer ${saved.token}`;
  } catch {
    /* corrupted storage: just send the request without a token */
  }
  return config;
});

// RESPONSE interceptor: runs on every response. We use it to turn the backend's
// different error shapes into ONE simple shape: { status, message, fields }.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status ?? 0;
    const body = error.response?.data;
    const hasBody = body && typeof body === 'object' && body.error;

    // The backend answers 401 for a missing/invalid/expired token and 403 for a logged-in
    // user who lacks permission. Only 401 on a request that sent a token means "session ended".
    // (A failed login is also a 401, but it carries no token, so it is not treated as expiry.)
    if (status === 401 && error.config?.headers?.Authorization) {
      window.dispatchEvent(new Event('auth:expired'));
    }

    let message = 'Something went wrong. Please try again.';
    if (hasBody) message = body.error;
    else if (status === 0) message = 'Cannot reach the server. Check your internet connection.';
    else if (status >= 500) message = 'The server is not responding. Is the backend running on port 8080?';

    // `fields` comes from @Valid failures: { email: "Must be a valid email address", ... }
    return Promise.reject({ status, message, fields: body?.fields || {} });
  }
);

export default api;
