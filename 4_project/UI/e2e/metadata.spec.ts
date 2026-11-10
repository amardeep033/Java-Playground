import { test, expect, selectColumn } from "./fixtures";
test.use({ actionTimeout: 10_000 });

test("search beyond the first page and edit safe resource metadata", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(90_000);
  const api = process.env.E2E_API_URL || "http://127.0.0.1:8081/api/v1";
  const user = (await (
    await request.post(`${api}/users`, {
      data: {
        name: "Metadata browser",
        email: `metadata-browser-${process.env.E2E_RUN_ID || Date.now()}@example.com`,
        password: "ExamplePass123!",
      },
    })
  ).json()) as { id: string };
  let app = { id: "" };
  for (let i = 0; i < 14; i++)
    app = (await (
      await request.post(`${api}/users/${user.id}/applications`, {
        data: { name: `Browser app ${String(i).padStart(2, "0")}` },
      })
    ).json()) as { id: string };
  const root = `/users/${user.id}/apps/${app.id}`;
  const type = (await (
    await request.post(`${api}/applications/${app.id}/event-types`, {
      data: { name: "metadata.browser", description: "Original definition" },
    })
  ).json()) as { id: string };
  await request.post(`${api}/applications/${app.id}/endpoints`, {
    data: {
      url: "https://example.com/metadata-browser",
      description: "Original destination",
      eventTypeIds: [type.id],
    },
  });
  await request.post(`${api}/applications/${app.id}/api-keys`, {
    data: { name: "Original machine key" },
  });
  await page.goto(`/users/${user.id}/applications`);
  await page.getByLabel("Search applications").fill("Browser app 13");
  await expect(
    page.getByRole("heading", { name: "Browser app 13", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByLabel("Name", { exact: true })
    .fill("Renamed browser app");
  await page
    .getByRole("dialog")
    .getByLabel("Description", { exact: true })
    .fill("Searchable metadata needle");
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(
    page.getByRole("heading", {
      name: "No matching applications",
      exact: true,
    }),
  ).toBeVisible();
  await page.getByLabel("Search applications").fill("metadata needle");
  await expect(
    page.getByRole("heading", { name: "Renamed browser app", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "Renamed browser app", exact: true })
    .click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Event types", exact: true })
    .click();
  await page.getByLabel("Search event types").fill("absent");
  await expect(
    page.getByRole("heading", { name: "No matching event types", exact: true }),
  ).toBeVisible();
  await page.getByLabel("Search event types").fill("metadata.browser");
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await expect(
    page.getByRole("dialog").getByLabel("Name", { exact: true }),
  ).toHaveCount(0);
  await page
    .getByRole("dialog")
    .getByLabel("Description", { exact: true })
    .fill("Updated definition needle");
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "Updated definition needle", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Endpoints", exact: true })
    .click();
  await page.getByLabel("Search endpoints").fill("metadata-browser");
  await selectColumn(page,"Status",["INACTIVE"]);
  await expect(
    page.getByRole("heading", { name: "No matching endpoints", exact: true }),
  ).toBeVisible();
  await selectColumn(page,"Status",["ACTIVE"]);
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await expect(page.getByRole("dialog").getByLabel("Destination URL",{exact:true})).toBeVisible();
  await expect(page.getByRole("dialog").getByLabel("Maximum delivery attempts",{exact:true})).toBeVisible();
  await page
    .getByRole("dialog")
    .getByLabel("Name", { exact: true })
    .fill("Renamed destination");
  await page
    .getByRole("dialog")
    .getByLabel("Description", { exact: true })
    .fill("Updated destination needle");
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(
    page.getByText("Updated destination needle", { exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("endpoints-search-edit.png"),
    fullPage: true,
  });
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "API keys", exact: true })
    .click();
  await page.getByLabel("Search API keys").fill("Original machine");
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByLabel("Name", { exact: true })
    .fill("Renamed machine");
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await page.getByLabel("Search API keys").fill("Renamed machine");
  await expect(
    page.getByRole("button", { name: "Renamed machine", exact: true }),
  ).toBeVisible();
  await selectColumn(page,"Status",["REVOKED"]);
  await expect(
    page.getByRole("heading", { name: "No matching api keys", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Deliveries", exact: true })
    .click();
  await expect(page.getByLabel("Search deliveries")).toBeVisible();
  await page.getByRole("button", { name: "Profile & security", exact: true })
    .click();
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByLabel("Name", { exact: true })
    .fill("Updated browser name");
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Updated browser name", exact: true }),
  ).toBeVisible();
  await page.getByLabel("Search login attempts").fill("127.0.0.1");
  await expect(
    page.getByRole("cell", { name: "Successful", exact: true }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/users/${user.id}/applications`);
  await page.getByLabel("Search applications").fill("metadata needle");
  await expect(
    page.getByRole("heading", { name: "Renamed browser app", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: testInfo.outputPath("applications-search-mobile.png"),
    fullPage: true,
  });
  expect(root).toContain(app.id);
});
