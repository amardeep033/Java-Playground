import { z } from "zod";
export const endpointSchema = z.object({
  url: z
    .string()
    .max(2048)
    .url("Enter a valid URL.")
    .refine((value) => {
      try {
        const url = new URL(value);
        return (
          ["http:", "https:"].includes(url.protocol) &&
          !url.username &&
          !url.password &&
          !url.hash &&
          url.port !== "0"
        );
      } catch {
        return false;
      }
    }, "Use an HTTP(S) URL without credentials or a fragment."),
  description: z.string().max(500).optional(),
  eventTypeIds: z
    .array(z.string().uuid())
    .min(1, "Select at least one event type.")
    .max(100),
});
export type EndpointValues = z.infer<typeof endpointSchema>;
