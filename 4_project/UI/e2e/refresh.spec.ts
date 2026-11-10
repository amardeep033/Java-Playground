import { test, expect } from "./fixtures";

test("renew expired access across tabs without exposing refresh credentials", async ({page,context,request}) => {
  const api=process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const response=await request.post(`${api}/users`,{data:{name:"Refresh browser",email:`refresh-${Date.now()}@example.com`,password:"ExamplePass123!"}});
  expect(response.status()).toBe(201);
  const user=await response.json() as {id:string};
  const path=`/users/${user.id}/applications`;
  await page.goto(path);
  await expect(page.getByRole("heading",{name:"Applications",exact:true})).toBeVisible();
  const other=await context.newPage();
  await other.goto(path);
  await expect(other.getByRole("heading",{name:"Applications",exact:true})).toBeVisible();
  const original=(await context.cookies()).find(cookie=>cookie.name==="ER_REFRESH")!;
  expect(original.httpOnly).toBe(true);
  let rotations=0;
  context.on("request",request=>{if(request.method()==="POST"&&request.url().endsWith("/auth/refresh")) rotations++;});
  // Browser cookie expiry removes access while leaving the longer-lived refresh cookie.
  await context.clearCookies({name:"ER_ACCESS"});
  await Promise.all([page.reload(),other.reload()]);
  await expect(page.getByRole("heading",{name:"Applications",exact:true})).toBeVisible();
  await expect(other.getByRole("heading",{name:"Applications",exact:true})).toBeVisible();
  expect(rotations).toBe(1);
  const cookies=await context.cookies();
  const rotated=cookies.find(cookie=>cookie.name==="ER_REFRESH")!;
  expect(rotated.value).not.toBe(original.value);
  expect(cookies.some(cookie=>cookie.name==="ER_ACCESS")).toBe(true);
  const exposed=await page.evaluate(()=>JSON.stringify({cookie:document.cookie,local:{...localStorage},session:{...sessionStorage}}));
  expect(exposed).not.toContain(original.value);
  expect(exposed).not.toContain(rotated.value);
  await page.getByRole("button",{name:"Profile & security"}).click();
  await page.getByRole("button",{name:"Sign out",exact:true}).click();
  await expect(page).toHaveURL("/");
  expect((await context.cookies()).some(cookie=>["ER_ACCESS","ER_REFRESH"].includes(cookie.name))).toBe(false);
  await other.close();
});
