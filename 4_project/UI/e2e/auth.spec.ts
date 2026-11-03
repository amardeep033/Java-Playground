import { test, expect } from "./fixtures";

test("login history, HttpOnly session persistence, and sign out", async ({
  page,
  request,
  context,
}, testInfo) => {
  const api = process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const email = `session-${process.env.E2E_RUN_ID || Date.now()}@example.com`;
  const password = "ExamplePass123!";
  const user = (await (
    await request.post(`${api}/users`, {
      data: { name: "Session test", email, password },
    })
  ).json()) as { id: string };
  await page.goto(`/users/${user.id}/settings`);
  await expect(
    page.getByRole("heading", { name: "Session & access", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("USER", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill("wrong-password");
  await page
    .getByRole("button", { name: "Sign in", exact: true })
    .last()
    .click();
  await expect(page.getByRole("alert")).toHaveText(
    "Email or password is incorrect.",
  );
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page
    .getByRole("button", { name: "Sign in", exact: true })
    .last()
    .click();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  const session = (await context.cookies()).find(
    (cookie) => cookie.name === "ER_SESSION",
  );
  expect(session?.httpOnly).toBe(true);
  expect(session?.sameSite).toBe("Lax");
  expect(await page.evaluate(() => document.cookie)).not.toContain(
    "ER_SESSION",
  );
  expect(await page.evaluate(() => JSON.stringify(localStorage))).not.toContain(
    session!.value,
  );
  expect(
    await page.evaluate(() => JSON.stringify(sessionStorage)),
  ).not.toContain(session!.value);
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Profile & security", exact: true })
    .click();
  await expect(
    page.getByRole("cell", { name: "Wrong email or password", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("cell", { name: "Successful", exact: true }),
  ).toHaveCount(2);
  await page.screenshot({
    path: testInfo.outputPath("profile-security.png"),
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Sign out all sessions", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Create your workspace", exact: true }),
  ).toBeVisible();
  await page.goto(`/users/${user.id}/settings`);
  await expect(
    page.getByRole("heading", { name: "Create your workspace", exact: true }),
  ).toBeVisible();
  // Revocation in another client must also clear a dashboard with cached session data.
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page
    .getByRole("button", { name: "Sign in", exact: true })
    .last()
    .click();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Profile & security", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Recent login attempts", exact: true }),
  ).toBeVisible();
  await request.post(`${api}/auth/logout-all`);
  await page
    .getByRole("button", { name: "Refresh history", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Create your workspace", exact: true }),
  ).toBeVisible();
});
