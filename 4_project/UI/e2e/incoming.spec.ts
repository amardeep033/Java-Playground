import { test, expect, selectColumn } from "./fixtures";

test("inspect accepted and rejected submissions without replay actions", async ({
  page,
  request,
}, testInfo) => {
  const api = process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const user = await (
    await request.post(api + "/users", {
      data: {
        name: "Incoming browser",
        email: `incoming-${process.env.E2E_RUN_ID}@example.com`,
        password: "ExamplePass123!",
      },
    })
  ).json();
  const app = await (
    await request.post(`${api}/users/${user.id}/applications`, {
      data: { name: "Incoming browser" },
    })
  ).json();
  const root = `${api}/applications/${app.id}`;
  const type = await (
    await request.post(root + "/event-types", {
      data: { name: "incoming.browser" },
    })
  ).json();
  const endpoint = await (
    await request.post(root + "/endpoints", {
      data: {
        url: "https://example.com/incoming-browser",
        eventTypeIds: [type.id],
      },
    })
  ).json();
  await request.put(root + `/endpoints/${endpoint.endpoint.id}/admin`, {
    data: { active: false },
  });
  const key = await (
    await request.post(root + "/api-keys", {
      data: { name: "Incoming browser" },
    })
  ).json();
  const rejected = await request.post(root + "/events", {
    headers: { "X-API-Key": key.secret },
    data: {
      eventType: "incoming.browser",
      payload: { private: "not-audit-data" },
    },
  });
  expect(rejected.status()).toBe(400);
  const requestId = rejected.headers()["x-request-id"];
  const accepted = await request.post(root + "/events", {
    headers: { "X-API-Key": key.secret, "Idempotency-Key": "browser-accepted" },
    data: { eventType: "incoming.browser", payload: {} },
  });
  expect(accepted.status()).toBe(202);
  await page.goto(`/users/${user.id}/apps/${app.id}/incoming-requests`);
  await expect(
    page.getByRole("heading", { name: "Incoming requests", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("row")).toHaveCount(3);
  await selectColumn(page,"Result",["REJECTED"]);
  await expect(page.getByRole("row")).toHaveCount(2);
  await page.getByLabel("Filter API key",{exact:true}).click();
  await page.getByLabel("Search API key options",{exact:true}).fill(key.key.name);
  await page.getByLabel("Filter API key",{exact:true}).click();
  await selectColumn(page,"Credential",["API_KEY"]);
  await selectColumn(page,"HTTP",["400"]);
  await expect(page.getByRole("row")).toHaveCount(2);

  await page
    .getByRole("button", { name: "View incoming request",exact:true })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(
    dialog.getByText("IDEMPOTENCY_KEY_MISSING", { exact: true }),
  ).toBeVisible();
  await expect(dialog.getByText(requestId, { exact: true })).toBeVisible();
  await expect(dialog.getByText("No", { exact: true })).toBeVisible();
  await expect(dialog.getByRole("button", { name: /replay/i })).toHaveCount(0);
  await expect(
    dialog.getByRole("button", { name: "View in events" }),
  ).toBeDisabled();
  await expect(dialog).not.toContainText("not-audit-data");
  await page.screenshot({
    path: testInfo.outputPath("incoming-rejection.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  await expect(dialog).toHaveCount(0);
  await selectColumn(page,"Credential",[]);
  await selectColumn(page,"HTTP",[]);
  await page.getByLabel("Filter API key",{exact:true}).click();
  await page.getByLabel("Search API key options",{exact:true}).fill("");
  await page.getByLabel("Filter API key",{exact:true}).click();
  await selectColumn(page,"Result",["ACCEPTED"]);
  await page
    .getByRole("button", { name: "View incoming request",exact:true })
    .click();
  await expect(
    dialog.getByText("INACTIVE_ENDPOINTS_SKIPPED", { exact: true }),
  ).toBeVisible();
  await expect(
    dialog.getByText(endpoint.endpoint.id, { exact: true }),
  ).toBeVisible();
  await expect(
    dialog.getByRole("button", { name: "View in events", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Close dialog", exact: true }).click();
  await expect(dialog).toHaveCount(0);
  await selectColumn(page,"Result",[]);
  await page
    .getByLabel("Search incoming requests", { exact: true })
    .fill(requestId);
  await expect(page.getByRole("row")).toHaveCount(2);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Incoming requests", exact: true }),
  ).toBeVisible();
  const receivedEvent = await accepted.json();
  await page.goto(
    `/users/${user.id}/apps/${app.id}/events/${receivedEvent.id}`,
  );
  await page.getByRole("dialog").getByText("Related records",{exact:true}).click();
  await page.getByRole("dialog").getByRole("button",{name:"View in incoming requests",exact:true}).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.locator("tbody tr td").nth(1).click();
  await expect(page.getByRole("dialog").getByRole("heading",{name:"Incoming request",exact:true})).toBeVisible();
  await expect(page.getByRole("dialog").getByText(accepted.headers()["x-request-id"],{exact:true})).toBeVisible();
});
