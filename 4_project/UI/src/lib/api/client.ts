import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

interface Problem {
  detail?: string;
  message?: string;
  errors?: Record<string, string>;
}
export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
    public fields: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  timeout: 15_000,
  withCredentials: true,
});
// This transport has no auth retry interceptors, avoiding recursive refresh/bootstrap calls.
const bootstrap = axios.create({ baseURL: client.defaults.baseURL, timeout: 15_000, withCredentials: true });
async function csrfHeader() {
  const { data } = await bootstrap.get<{ token: string; headerName: string }>("/auth/csrf");
  return { [data.headerName]: data.token };
}
let renewal: Promise<void> | undefined;
async function renewAccess() {
  if (!renewal) {
    const renew = async () => {
      // Another tab/request may already have renewed the shared cookie while we waited.
      try { await bootstrap.get("/auth/me"); return; }
      catch (error) { if (!axios.isAxiosError(error) || error.response?.status !== 401) throw error; }
      await bootstrap.post("/auth/refresh", null, { headers: await csrfHeader() });
    };
    // Serialize rotation across tabs as well as within this tab to avoid accidental reuse.
    renewal = (navigator.locks ? navigator.locks.request("event-relay-auth-refresh", renew) : renew())
      .finally(() => { renewal = undefined; });
  }
  return renewal;
}
client.interceptors.request.use(async (config) => {
  config.headers.Accept = "application/json";
  if (!["get", "head", "options"].includes(config.method || "get") && !config.headers.has("X-API-Key")) {
    // No JS token cache: obtain a fresh masked CSRF token before every cookie write.
    for (const [name, value] of Object.entries(await csrfHeader())) config.headers.set(name, value);
  }
  return config;
});
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<Problem>) => {
    const status = error.response?.status;
    const fallback: Record<number, string> = {
      400: "Check the highlighted fields and try again.",
      401: error.config?.url?.endsWith("/password")
        ? "The current password is incorrect."
        : "Email or password is incorrect, or your session has expired.",
      403: "You do not have access to this resource.",
      429: "Too many login attempts. Try again in 15 minutes.",
      404: "This resource could not be found.",
      409: "This record already exists, or this API key has been revoked.",
    };
    const config = error.config as (InternalAxiosRequestConfig & { authRetried?: boolean }) | undefined;
    const explicitKey = config?.headers.has("X-API-Key");
    const publicAuth = /\/auth\/(?:login|logout|refresh|csrf)$/.test(config?.url || "") || config?.url === "/users";
    const passwordError = config?.url?.endsWith("/password") && !error.response?.headers["www-authenticate"];
    if (status === 401 && config && !explicitKey && !publicAuth && !passwordError) {
      if (!config.authRetried) {
        config.authRetried = true;
        try { await renewAccess(); }
        catch (refreshError) {
          // A transient outage must not discard the user's login or replay writes.
          if (!axios.isAxiosError(refreshError) || refreshError.response?.status !== 401) {
            throw new ApiError("Could not renew your login. Please try again.", axios.isAxiosError(refreshError) ? refreshError.response?.status : undefined);
          }
          window.dispatchEvent(new Event("session-expired"));
          throw new ApiError("Your login has expired. Please sign in again.", 401);
        }
        return client.request(config);
      }
      window.dispatchEvent(new Event("session-expired"));
    }
    return Promise.reject(
      new ApiError(
        error.response?.data?.detail ||
          (status
            ? fallback[status] || "Something went wrong. Please try again."
            : "Cannot reach Service A. Check that it is running, then retry."),
        status,
        error.response?.data?.errors,
      ),
    );
  },
);
