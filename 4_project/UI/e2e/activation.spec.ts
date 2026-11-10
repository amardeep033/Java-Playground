import { test, expect } from "./fixtures";
test("pause and reactivate resources from their pencil settings", async ({page,request})=>{
  const api=process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const user=await(await request.post(api+"/users",{data:{name:"Activation browser",email:`activation-browser-${process.env.E2E_RUN_ID}@example.com`,password:"ExamplePass123!"}})).json();
  const app=await(await request.post(`${api}/users/${user.id}/applications`,{data:{name:"Activation browser app"}})).json();
  const type=await(await request.post(`${api}/applications/${app.id}/event-types`,{data:{name:"activation.browser"}})).json();
  await request.post(`${api}/applications/${app.id}/endpoints`,{data:{url:"https://example.com/activation-browser",eventTypeIds:[type.id]}});
  const root=`/users/${user.id}`;
  for(const [path,name] of [[`${root}/applications`,app.name],[`${root}/apps/${app.id}/event-types`,type.name],[`${root}/apps/${app.id}/endpoints`,"https://example.com/activation-browser"]]){
    await page.goto(path);
    await expect(page.getByRole("button",{name:`Deactivate ${name}`,exact:true})).toHaveCount(0);
    await page.getByRole("button",{name:"Edit",exact:true}).click();
    await page.getByRole("switch",{name:`Activate ${name}`,exact:true}).click();
    await expect(page.getByRole("dialog").getByText("Inactive",{exact:true})).toBeVisible();
    await page.getByRole("switch",{name:`Activate ${name}`,exact:true}).click();
    await expect(page.getByRole("dialog").getByText("Active",{exact:true})).toBeVisible();
    await page.keyboard.press("Escape");
  }
});
