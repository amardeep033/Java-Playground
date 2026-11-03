import axios, { AxiosError } from "axios";

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
let csrfRequest: Promise<string> | undefined;
// Fetch once before writes; read the current cookie after login rotates it.
function csrfCookie() {
  return document.cookie
    .split("; ")
    .find((value) => value.startsWith("XSRF-TOKEN="))
    ?.slice(11);
}
client.interceptors.request.use(async (config) => {
  if (!["get", "head", "options"].includes(config.method || "get")) {
    let token = csrfCookie();
    if (!token) {
      csrfRequest ??= axios
        .get<{ token: string }>(`${client.defaults.baseURL}/auth/csrf`, {
          withCredentials: true,
        })
        .then((response) => response.data.token)
        .finally(() => {
          csrfRequest = undefined;
        });
      token = await csrfRequest;
    }
    config.headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
  }
  config.headers.Accept = "application/json";
  return config;
});
client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<Problem>) => {
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
    if (
      status === 401 &&
      !error.config?.url?.endsWith("/auth/login") &&
      !error.config?.url?.endsWith("/auth/csrf") &&
      !error.config?.url?.endsWith("/password")
    ) {
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
