import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { ErrorMessage } from "@/components/shared/feedback";
import type { Endpoint } from "@/types/api";
import { endpointsApi } from "./api";
const schema = z.object({
  rateLimitPerSecond: z.number().int().min(1).max(100000),
  concurrencyLimit: z.number().int().min(1).max(1000),
  timeoutMs: z.number().int().min(100).max(60000),
  maxAttempts: z.number().int().min(1).max(20),
});
type Values = z.infer<typeof schema>;
export function ConfigurationForm({
  endpoint,
  onSaved,
}: {
  endpoint: Endpoint;
  onSaved: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: endpoint.configuration,
  });
  const mutation = useMutation({
    mutationFn: (value: Values) =>
      endpointsApi.configure(endpoint.applicationId, endpoint.id, value),
    onSuccess: onSaved,
  });
  const fields = [
    {
      key: "rateLimitPerSecond",
      label: "Rate limit (requests / second)",
      min: 1,
      max: 100000,
    },
    {
      key: "concurrencyLimit",
      label: "Concurrent requests",
      min: 1,
      max: 1000,
    },
    {
      key: "timeoutMs",
      label: "Request timeout (milliseconds)",
      min: 100,
      max: 60000,
    },
    { key: "maxAttempts", label: "Maximum delivery attempts", min: 1, max: 20 },
  ] as const;
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <div className="form-columns">
        {fields.map((field) => (
          <Field
            key={field.key}
            id={field.key}
            label={field.label}
            error={errors[field.key]?.message}
          >
            <Input
              id={field.key}
              type="number"
              min={field.min}
              max={field.max}
              {...register(field.key, { valueAsNumber: true })}
            />
          </Field>
        ))}
      </div>
      <p className="field-hint">
        Settings update in place and apply to newly accepted requests.
        Queued work keeps its original configuration.
      </p>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button disabled={mutation.isPending}>Save configuration</Button>
      </div>
    </form>
  );
}
