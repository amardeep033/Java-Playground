import { test, expect } from "./fixtures";
test("register and configure an application, then rotate credentials", async ({
  page,
}, testInfo) => {
  const email = `ui-e2e-${process.env.E2E_RUN_ID || Date.now()}@example.com`;
  const browserErrors: string[] = [];
  page.on("pageerror", (error) => browserErrors.push(error.message));
  await page.goto("/");
  await expect(page.getByLabel("Full name")).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("welcome.png"),
    fullPage: true,
  });
  await page.getByLabel("Full name").fill("UI Test User");
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill("ExamplePass123!");
  await page.getByRole("button", { name: "Create your workspace" }).click();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Create application", exact: true })
    .first()
    .click();
  await page.getByLabel("Application name").fill("E2E application");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Create application" })
    .click();
  await expect(
    page.getByRole("heading", { name: "E2E application", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("overview.png"),
    fullPage: true,
  });
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Event types", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Create event type", exact: true })
    .click();
  await page.getByLabel("Event type name").fill("order.created");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Create event type" })
    .click();
  await expect(
    page.getByRole("button", { name: "order.created" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "order.created" }).click();
  await expect(
    page.getByRole("dialog").getByText("Event type ID", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Endpoints", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Create endpoint", exact: true })
    .click();
  await page
    .getByLabel("Destination URL")
    .fill("https://example.com/webhooks/e2e");
  await page.getByRole("checkbox", { name: "order.created" }).check();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Create endpoint" })
    .click();
  const signingSecret = await page.getByTestId("signing-secret").textContent();
  expect(signingSecret).toMatch(/^whsec_/);
  await page.getByRole("button", { name: "I’ve saved the secret" }).click();
  await expect(page.getByTestId("signing-secret")).toHaveCount(0);
  await page.getByRole("button", { name: "Configure", exact: true }).click();
  await page.getByLabel("Rate limit (requests / second)").fill("25");
  await page.getByLabel("Concurrent requests").fill("8");
  await page.getByLabel("Request timeout (milliseconds)").fill("15000");
  await page.getByLabel("Maximum delivery attempts").fill("3");
  await page.getByRole("button", { name: "Save configuration" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "Configure", exact: true }).click();
  await expect(page.getByLabel("Rate limit (requests / second)")).toHaveValue(
    "25",
  );
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page
    .getByRole("button", { name: "Rotate secret", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Rotate signing secret", exact: true })
    .click();
  await expect(page.getByTestId("signing-secret")).toBeVisible();
  expect(await page.getByTestId("signing-secret").textContent()).not.toBe(
    signingSecret,
  );
  await page.getByRole("button", { name: "I’ve saved the secret" }).click();

  await expect(
    page.getByRole("button", { name: /https:\/\/example.com\/webhooks\/e2e/ }),
  ).toBeVisible();
  await page.getByRole("button", { name: "View details" }).click();
  await expect(
    page.getByRole("dialog").getByText("Subscribed event type IDs"),
  ).toBeVisible();
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "API keys", exact: true })
    .click();
  await page.getByRole("button", { name: "Generate API key" }).click();
  await page.getByLabel("Key name").fill("E2E key");
  await page.getByRole("button", { name: "Generate key", exact: true }).click();
  const firstSecret = await page.getByTestId("one-time-secret").textContent();
  expect(firstSecret).toMatch(/^er_/);
  await page.getByRole("button", { name: "I’ve saved my key" }).click();
  await expect(page.getByTestId("one-time-secret")).toHaveCount(0);
  await page.getByRole("button", { name: "E2E key", exact: true }).click();
  await expect(
    page.getByRole("dialog").getByText("Key ID", { exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("dialog")).not.toContainText(firstSecret!);
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page.getByRole("button", { name: "Rotate", exact: true }).click();
  await page.getByRole("button", { name: "Rotate key", exact: true }).click();
  await expect(page.getByTestId("one-time-secret")).toBeVisible();
  expect(await page.getByTestId("one-time-secret").textContent()).not.toBe(
    firstSecret,
  );
  await page.getByRole("button", { name: "I’ve saved my key" }).click();
  const activeRow = page.getByRole("row").filter({ hasText: "Active" });
  await activeRow.getByRole("button", { name: "Revoke", exact: true }).click();
  await page.getByRole("button", { name: "Revoke key", exact: true }).click();
  await expect(page.getByText("Revoked", { exact: true })).toHaveCount(2);
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Profile & security" })
    .click();
  await page
    .getByLabel("Current password", { exact: true })
    .fill("ExamplePass123!");
  await page
    .getByLabel("New password", { exact: true })
    .fill("ChangedPass123!");
  await page
    .getByLabel("Confirm new password", { exact: true })
    .fill("ChangedPass123!");
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(
    page.getByRole("heading", { name: "Create your workspace", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill("ChangedPass123!");
  await page
    .getByRole("button", { name: "Sign in", exact: true })
    .last()
    .click();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Switch to dark theme" }).click();
  await expect(page.locator("html")).toHaveClass("dark");
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Open navigation" }).click();
  await expect(page.getByRole("navigation")).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Applications", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Applications", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("mobile-dark.png"),
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  expect(
    await page.evaluate(() =>
      Object.keys(localStorage).filter((key) => key !== "event-relay-theme"),
    ),
  ).toEqual([]);

  await page.setViewportSize({ width: 1280, height: 900 });
  await page
    .getByRole("link", { name: "Open application", exact: true })
    .click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Endpoints", exact: true })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Confirm delete" }).click();
  await expect(
    page.getByRole("heading", { name: "Add your first destination" }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Event types", exact: true })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Confirm delete" }).click();
  await expect(
    page.getByRole("heading", { name: "Give your events a name" }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Applications", exact: true })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Confirm delete" }).click();
  await expect(
    page.getByRole("heading", { name: "Your first connection starts here" }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Profile & security" })
    .click();
  await page
    .getByRole("button", { name: "Delete account", exact: true })
    .click();
  await page.getByRole("button", { name: "Confirm delete" }).click();
  await expect(
    page.getByRole("heading", { name: "Create your workspace", exact: true }),
  ).toBeVisible();
  expect(browserErrors).toEqual([]);
});
test("mobile login validates credentials without horizontal overflow", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page
    .getByRole("button", { name: "Sign in", exact: true })
    .last()
    .click();
  await expect(
    page.getByText("Enter a valid email address.", { exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
