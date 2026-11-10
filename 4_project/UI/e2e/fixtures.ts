import { test as base, expect, type APIRequestContext } from "@playwright/test";
export const test = base.extend({
  request: async ({ context }, use) => {
    const original = context.request;
    const api = process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
    const request = new Proxy(original, {
      get(target, property) {
        const value = Reflect.get(target, property);
        if (["post", "put", "patch", "delete"].includes(String(property)))
          return async (url: string, options: Record<string, unknown> = {}) => {
            const headers = {
              ...((options.headers as Record<string, string>) || {}),
            };
            if (url.startsWith(api + "/") && !("X-API-Key" in headers)) {
              const csrf = await (await target.get(`${api}/auth/csrf`)).json();
              headers[csrf.headerName] = csrf.token;
            }
            const data = url===api+"/users" && options.data && typeof options.data==="object" ? {companyName:"Browser company",...options.data} : options.data;
            return value.call(target, url, { ...options, data, headers });
          };
        return typeof value === "function" ? value.bind(target) : value;
      },
    }) as APIRequestContext;
    await use(request);
  },
});
export { expect };

export async function selectColumn(page:import("@playwright/test").Page,label:string,values:string[]) {
 const trigger=page.getByLabel(`Filter ${label}`,{exact:true});
 const panel=trigger.locator("..");
 if(!(await panel.evaluate(element=>(element as HTMLDetailsElement).open)))await trigger.click();
 await panel.getByRole("button",{name:"Clear selection",exact:true}).click();
 for(const value of values)await panel.getByRole("checkbox",{name:new RegExp(`^${value}$`,"i")}).check();
 await trigger.click();
}
