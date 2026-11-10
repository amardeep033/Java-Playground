import { useState } from "react";
import { useTracking } from "./tracking-context";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Send } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { Field, Input, Textarea } from "@/components/ui/field";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { eventTypesApi } from "@/features/events/api";
import { activityApi } from "./api";
import type { JsonValue } from "./types";
export function parsePayload(text: string): JsonValue {
  if (new TextEncoder().encode(text).length > 1_000_000)
    throw new Error("Payload must be at most 1 MB.");
  const value: JsonValue = JSON.parse(text) as JsonValue;
  if (value === null) throw new Error("Payload cannot be null.");
  return value;
}
const schema = z.object({
  eventType: z.string().min(1, "Select an event type."),
  payload: z.string().superRefine((text, ctx) => {
    try {
      parsePayload(text);
    } catch (error) {
      ctx.addIssue({
        code: "custom",
        message:
          error instanceof SyntaxError
            ? "Enter valid JSON."
            : error instanceof Error
              ? error.message
              : "Enter valid JSON.",
      });
    }
  }),
});
type Values = z.infer<typeof schema>;
function SendForm({ onClose }: { onClose: () => void }) {
  const { app } = useWorkspace();
  const [typePage, setTypePage] = useState(0);
  const [requestKey] = useState(() => crypto.randomUUID());
  const [selectedName, setSelectedName] = useState("");
  const queryClient = useQueryClient();
  const {open}=useTracking();
  const types = useQuery({
    queryKey: ["event-types", app!.id, "send", typePage],
    queryFn: () =>
      eventTypesApi.list(app!.id, {
        page: typePage,
        size: 20,
        status: "ACTIVE",
      }),
  });
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      eventType: "",
      payload: '{\n  "orderId": "test-001",\n  "amount": 500\n}',
    },
  });
  const mutation = useMutation({
    mutationFn: (value: Values) =>
      activityApi.send(app!.id, requestKey, {
        eventType: value.eventType,
        payload: parsePayload(value.payload),
      }),
    onSuccess: (receipt) => {
      void queryClient.invalidateQueries({ queryKey: ["activity", app!.id] });
      onClose();
      open({kind:"event",id:receipt.id});
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <Field
        id="send-event-type"
        label="Event type"
        error={errors.eventType?.message}
      >
        <select
          className="input"
          id="send-event-type"
          {...register("eventType", {
            onChange: (event) => setSelectedName(String(event.target.value)),
          })}
        >
          <option value="">Select a registered event type</option>
          {selectedName &&
            !types.data?.items.some((type) => type.name === selectedName) && (
              <option value={selectedName}>{selectedName}</option>
            )}
          {types.data?.items.map((type) => (
            <option key={type.id} value={type.name}>
              {type.name}
            </option>
          ))}
        </select>
      </Field>
      {types.isPending ? (
        <Loading />
      ) : types.isError ? (
        <ErrorMessage error={types.error} retry={() => void types.refetch()} />
      ) : types.data.totalElements === 0 ? (
        <p className="field-hint">
          Create an event type and subscribe an endpoint before sending.
        </p>
      ) : types.data.totalPages > 1 ? (
        <div className="pagination">
          <span>
            Event types · {typePage + 1}/{types.data.totalPages}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!typePage}
            onClick={() => setTypePage(typePage - 1)}
          >
            Previous types
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={typePage + 1 >= types.data.totalPages}
            onClick={() => setTypePage(typePage + 1)}
          >
            Next types
          </Button>
        </div>
      ) : null}
      <Field
        id="event-payload"
        label="JSON payload"
        error={errors.payload?.message}
        hint="Sent to every enabled endpoint subscribed to this event type."
      >
        <Textarea
          id="event-payload"
          className="input textarea payload-editor"
          rows={9}
          spellCheck={false}
          {...register("payload")}
        />
      </Field>
      <Field
        id="send-request-id"
        label="Idempotency key"
        hint="Retained if this request fails, so retrying does not create duplicate deliveries."
      >
        <Input id="send-request-id" value={requestKey} readOnly />
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button type="button" variant="outline" onClick={onClose}>
          Cancel
        </Button>
        <Button
          disabled={
            mutation.isPending || !app?.active || !types.data?.totalElements
          }
        >
          <Send size={15} />
          {mutation.isPending ? "Sending…" : "Send event"}
        </Button>
      </div>
    </form>
  );
}
export function SendEventButton() {
  const { app } = useWorkspace();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button
        disabled={!app?.active}
        title={
          !app?.active ? "Activate the application before sending" : undefined
        }
        onClick={() => setOpen(true)}
      >
        <Send size={16} />
        Send event
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader
            title="Send an event"
            description="Publish an event through Service A, RabbitMQ, and your subscribed endpoints."
          />
          {open && <SendForm onClose={() => setOpen(false)} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
