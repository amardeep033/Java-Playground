import { test as base, expect, type APIRequestContext } from "@playwright/test";
export const test = base.extend({
  request: async ({ context }, use) => {
    const original = context.request;
    const request = new Proxy(original, {
      get(target, property) {
        const value = Reflect.get(target, property);
        if (["post", "put", "patch", "delete"].includes(String(property)))
          return async (url: string, options: Record<string, unknown> = {}) => {
            const api =
              process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
            const csrf = (await (
              await target.get(`${api}/auth/csrf`)
            ).json()) as { token: string };
            return value.call(target, url, {
              ...options,
              headers: {
                ...((options.headers as object) || {}),
                "X-XSRF-TOKEN": csrf.token,
              },
            });
          };
        return typeof value === "function" ? value.bind(target) : value;
      },
    }) as APIRequestContext;
    await use(request);
  },
});
export { expect };
