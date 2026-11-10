import { describe, expect, it } from "vitest";
import { endpointSchema } from "./schemas";
const eventTypeIds = ["c212fb6d-751a-4893-a559-a7389f68d276"];
describe("endpoint validation", () => {
  it.each([
    "file:///tmp/hook",
    "ftp://example.com/hook",
    "https://user:pass@example.com/hook",
    "https://example.com/hook#fragment",
  ])("rejects unsupported URL %s", (url) => {
    expect(
      endpointSchema.safeParse({ name: "Test endpoint", url, eventTypeIds })
        .success,
    ).toBe(false);
  });
  it("requires an event subscription", () => {
    expect(
      endpointSchema.safeParse({
        name: "Test endpoint",
        url: "https://example.com/hook",
        eventTypeIds: [],
      }).success,
    ).toBe(false);
  });
  it("accepts HTTPS with valid event types", () => {
    expect(
      endpointSchema.safeParse({
        name: "Test endpoint",
        url: "https://example.com/hook",
        eventTypeIds,
      }).success,
    ).toBe(true);
  });
});
