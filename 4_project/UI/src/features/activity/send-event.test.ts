import { describe, expect, it } from "vitest";
import { parsePayload } from "./send-event";
describe("event payload validation", () => {
  it("parses nested JSON without changing payload values", () => {
    expect(parsePayload('{"id":"123","items":[true,3]}')).toEqual({
      id: "123",
      items: [true, 3],
    });
  });
  it("rejects malformed JSON", () => {
    expect(() => parsePayload("{invalid")).toThrow();
  });
  it("rejects a null payload", () => {
    expect(() => parsePayload("null")).toThrow("Payload cannot be null");
  });
  it("checks UTF-8 byte length rather than character count", () => {
    expect(() => parsePayload(JSON.stringify("😀".repeat(260000)))).toThrow(
      "1 MB",
    );
  });
});
