import { z } from "zod";
export const registrationSchema = z.object({
  companyName: z.string().trim().min(1, "Enter your company name.").max(120),
  name: z.string().trim().min(1, "Enter your name.").max(120),
  email: z.string().email("Enter a valid email address.").max(320),
  password: z.string().min(12, "Use at least 12 characters.").max(128),
});
export type RegistrationValues = z.infer<typeof registrationSchema>;
export const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Enter your current password.").max(128),
    newPassword: z.string().min(12, "Use at least 12 characters.").max(128),
    confirmation: z.string(),
  })
  .refine((value) => value.newPassword === value.confirmation, {
    path: ["confirmation"],
    message: "Passwords do not match.",
  })
  .refine((value) => value.currentPassword !== value.newPassword, {
    path: ["newPassword"],
    message: "Choose a different password.",
  });
export type PasswordValues = z.infer<typeof passwordSchema>;
