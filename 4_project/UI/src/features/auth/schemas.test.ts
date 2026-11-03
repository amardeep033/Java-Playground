import { describe, expect, it } from "vitest";
import { passwordSchema, registrationSchema } from "./schemas";
describe("credential validation", () => {
  it("rejects short registration passwords", () => {
    expect(
      registrationSchema.safeParse({
        name: "Alex",
        email: "alex@example.com",
        password: "short",
      }).success,
    ).toBe(false);
  });
  it("rejects an invalid registration email", () => {
    expect(
      registrationSchema.safeParse({
        name: "Alex",
        email: "invalid",
        password: "LongEnough123!",
      }).success,
    ).toBe(false);
  });
  it("requires matching new password confirmation", () => {
    const result = passwordSchema.safeParse({
      currentPassword: "Original123!",
      newPassword: "ChangedPass123!",
      confirmation: "OtherPass123!",
    });
    expect(result.success).toBe(false);
    if (!result.success)
      expect(result.error.issues[0].path).toContain("confirmation");
  });
  it("rejects reuse of the current password", () => {
    expect(
      passwordSchema.safeParse({
        currentPassword: "OriginalPass123!",
        newPassword: "OriginalPass123!",
        confirmation: "OriginalPass123!",
      }).success,
    ).toBe(false);
  });
});
