import { test, expect, selectColumn } from "./fixtures";
import type { Application, IssuedApiKey, User } from "../src/types/api";
import type { DeliveryDetails } from "../src/features/activity/types";

test("send, monitor attempts, replay a failed delivery and browse history", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(90_000);
  const api = process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const receiver = process.env.E2E_RECEIVER_URL;
  test.skip(
    !receiver,
    "Run e2e/run-local.py to supply the isolated HTTP receiver and both services.",
  );
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const user = (await (
    await request.post(`${api}/users`, {
      data: {
        name: "Activity test",
        email: `activity-${process.env.E2E_RUN_ID || Date.now()}@example.com`,
        password: "ExamplePass123!",
      },
    })
  ).json()) as User;
  const app = (await (
    await request.post(`${api}/users/${user.id}/applications`, {
      data: { name: "Activity workspace" },
    })
  ).json()) as Application;
  const issued = (await (
    await request.post(`${api}/applications/${app.id}/api-keys`, {
      data: { name: "Activity key" },
    })
  ).json()) as IssuedApiKey;
  const type = (await (
    await request.post(`${api}/applications/${app.id}/event-types`, {
      data: { name: "ui.test" },
    })
  ).json()) as { id: string };
  await request.post(`${api}/applications/${app.id}/endpoints`, {
    data: {
      url: `${receiver}/webhook`,
      eventTypeIds: [type.id],
      configuration: {
        enabled: true,
        rateLimitPerSecond: 10,
        concurrencyLimit: 4,
        timeoutMs: 2000,
        maxAttempts: 2,
      },
    },
  });
  await request.post(`${receiver}/control`, {
    data: { path: "/webhook", status: 503 },
  });
  const root = `/users/${user.id}/apps/${app.id}`;
  await page.goto(`${root}/events`);
  await expect(
    page.getByRole("heading", { name: "Your first event starts here" }),
  ).toBeVisible();
  await expect(page.getByText("Event explorer", { exact: true })).toHaveCount(
    0,
  );
  await expect(
    page.getByText("Signed in with your workspace session", { exact: true }),
  ).toHaveCount(0);
  expect(
    (await page.locator(".activity-toolbar").boundingBox())!.y,
  ).toBeLessThan(320);
  await page.screenshot({
    path: testInfo.outputPath("events-compact-desktop.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Send event", exact: true }).click();
  await page.getByLabel("Event type", { exact: true }).selectOption("ui.test");
  await page.getByLabel("JSON payload", { exact: true }).fill("{invalid");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Send event", exact: true })
    .click();
  await expect(
    page.getByText("Enter valid JSON.", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("JSON payload", { exact: true }).fill(
    JSON.stringify({
      orderId: "ui-order-001",
      message: "<script>not executable</script>",
    }),
  );
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Send event", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Event details", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("dialog").getByText("<script>not executable</script>", { exact: false })).toBeVisible();
  const eventId = new URL(page.url()).searchParams.get("inspectId")!;
  await page.getByRole("dialog").getByText("Related records",{exact:true}).click();
  await page.getByRole("dialog").getByRole("button",{name:"View in deliveries",exact:true}).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("cell", { name: "Failed", exact: true })).toBeVisible({timeout:20_000});
  await page.getByRole("button",{name:"View delivery",exact:true}).first().click();
  await expect(
    page.getByRole("heading", { name: "Delivery details", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".tracking-dialog").getByText("Attempt 1", { exact: true })).toBeVisible();
  await expect(page.locator(".tracking-dialog").getByText("Attempt 2", { exact: true })).toBeVisible();
  await expect(page.locator("details[open] pre")).toContainText(
    "temporary failure",
  );
  const originalId = new URL(page.url()).searchParams.get("inspectId")!;
  await page.screenshot({
    path: testInfo.outputPath("failed-delivery.png"),
    fullPage: true,
  });
  await request.post(`${receiver}/control`, {
    data: { path: "/webhook", status: 200 },
  });
  await page.getByRole("button", { name: "Replay", exact: true }).click();
  await page
    .getByRole("button", { name: "Confirm replay", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  const replayId = new URL(page.url()).searchParams.get("q")!;
  expect(replayId).not.toBe(originalId);
  await page.locator("tr.selected-row").getByRole("button",{name:"View delivery",exact:true}).click();
  await expect(page.locator(".tracking-dialog").getByText("Succeeded", { exact: true })).toBeVisible({timeout:20_000});
  await page.locator(".tracking-dialog").getByText("Related records",{exact:true}).click();
  await expect(page.locator(".tracking-dialog").getByRole("button",{name:"View original in deliveries",exact:true})).toBeVisible();
  await page.locator(".tracking-dialog").getByText("Related records",{exact:true}).click();
  await expect(page.locator(".tracking-dialog").getByText("accepted", { exact: false })).toBeVisible();
  const original = (await (
    await request.get(
      `${api}/applications/${app.id}/deliveries/${originalId}`,
      { headers: { "X-API-Key": issued.secret } },
    )
  ).json()) as DeliveryDetails;
  expect(original.delivery.status).toBe("DEAD");
  expect(original.delivery.replayId).toBe(replayId);
  expect(original.attempts).toHaveLength(2);
  await page.getByRole("button",{name:"Close dialog",exact:true}).click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Dead letters", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "No failed deliveries", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("checkbox", { name: "Include replayed", exact: true })
    .check();
  await expect(
    page.getByRole("button", { name: "View replay", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Events", exact: true })
    .click();
  await selectColumn(page,"Status",["SUCCEEDED"]);
  await expect(
    page.getByRole("cell", { name: "Succeeded", exact: true }),
  ).toBeVisible();
  await page
    .getByLabel("Search events", { exact: true })
    .fill("does-not-exist");
  await expect(
    page.getByRole("heading", { name: "No matching events", exact: true }),
  ).toBeVisible();
  await page.getByLabel("Search events", { exact: true }).fill("ui.test");
  await expect(
    page.getByRole("cell", { name: "ui.test", exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: testInfo.outputPath("events-mobile.png"),
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(await page.evaluate(() => JSON.stringify(localStorage))).not.toContain(
    issued.secret,
  );
  expect(
    await page.evaluate(() => JSON.stringify(sessionStorage)),
  ).not.toContain(issued.secret);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Events", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("cell", { name: "ui.test", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByLabel("Application API key", { exact: true }),
  ).toHaveCount(0);
  expect(errors).toEqual([]);
  expect(eventId).toMatch(/^[0-9a-f-]{36}$/);
});
