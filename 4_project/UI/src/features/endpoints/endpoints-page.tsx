import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import { Globe2, Plus, KeyRound } from "lucide-react";
import { endpointsApi } from "./api";
import { endpointSchema, type EndpointValues } from "./schemas";
import { eventTypesApi } from "@/features/events/api";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import {
  CopyButton,
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { Field, Input, Textarea } from "@/components/ui/field";
import { Checkbox } from "@/components/ui/checkbox";
import { DeleteResource } from "@/components/shared/delete-resource";
import { ConfigurationForm } from "./configuration-form";
import type { Endpoint, IssuedEndpoint } from "@/types/api";
import { date } from "@/lib/utils";

function CreateEndpoint({
  onClose,
  onIssued,
}: {
  onClose: () => void;
  onIssued: (issued: IssuedEndpoint) => void;
}) {
  const { app, user } = useWorkspace();
  const queryClient = useQueryClient();
  const [eventPage, setEventPage] = useState(0);
  const events = useQuery({
    queryKey: ["event-types", app!.id, "selection", eventPage],
    queryFn: () => eventTypesApi.list(app!.id, { page: eventPage, size: 8 }),
  });
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<EndpointValues>({
    resolver: zodResolver(endpointSchema),
    defaultValues: { eventTypeIds: [] },
  });
  const selected = watch("eventTypeIds");
  const mutation = useMutation({
    mutationFn: async (value: EndpointValues) => {
      const issued = await endpointsApi.create(app!.id, value);
      onIssued(issued);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["endpoints", app!.id] });
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <Field
        id="endpoint-url"
        label="Destination URL"
        error={errors.url?.message}
      >
        <Input
          id="endpoint-url"
          type="url"
          placeholder="https://api.example.com/webhooks"
          autoFocus
          {...register("url")}
        />
      </Field>
      <Field
        id="endpoint-description"
        label="Description (optional)"
        error={errors.description?.message}
      >
        <Textarea
          id="endpoint-description"
          placeholder="What receives events here?"
          {...register("description")}
        />
      </Field>
      <fieldset className="subscription-fieldset">
        <legend>
          Subscribe to event types{" "}
          <span className="count-badge">{selected.length} selected</span>
        </legend>
        {events.isPending ? (
          <Loading />
        ) : events.isError ? (
          <ErrorMessage
            error={events.error}
            retry={() => void events.refetch()}
          />
        ) : events.data.totalElements === 0 ? (
          <p className="field-hint">
            Create an event type first.{" "}
            <Link
              className="text-link"
              to={`/users/${user.id}/apps/${app!.id}/event-types`}
            >
              Go to event types
            </Link>
          </p>
        ) : (
          <>
            <div className="subscription-list">
              {events.data.items.map((event) => (
                <label className="subscription-option" key={event.id}>
                  <Checkbox
                    checked={selected.includes(event.id)}
                    onCheckedChange={(checked) =>
                      setValue(
                        "eventTypeIds",
                        checked === true
                          ? [...selected, event.id]
                          : selected.filter((id) => id !== event.id),
                        { shouldValidate: true },
                      )
                    }
                  />
                  <span>
                    <code>{event.name}</code>
                    {event.description && <small>{event.description}</small>}
                  </span>
                </label>
              ))}
            </div>
            {events.data.totalPages > 1 && (
              <div className="pagination">
                <span>
                  Page {eventPage + 1} of {events.data.totalPages}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={eventPage === 0}
                  onClick={() => setEventPage(eventPage - 1)}
                >
                  Previous types
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={eventPage + 1 >= events.data.totalPages}
                  onClick={() => setEventPage(eventPage + 1)}
                >
                  Next types
                </Button>
              </div>
            )}
          </>
        )}
        {errors.eventTypeIds && (
          <p role="alert" className="field-error">
            {errors.eventTypeIds.message}
          </p>
        )}
      </fieldset>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button type="button" variant="outline" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={mutation.isPending || !events.data?.totalElements}>
          Create endpoint
        </Button>
      </div>
    </form>
  );
}
function EndpointDetails({ id }: { id: string }) {
  const { app } = useWorkspace();
  const query = useQuery({
    queryKey: ["endpoint", app!.id, id],
    queryFn: () => endpointsApi.get(app!.id, id),
  });
  return query.isPending ? (
    <Loading />
  ) : query.isError ? (
    <ErrorMessage error={query.error} retry={() => void query.refetch()} />
  ) : (
    <dl className="detail-list">
      <dt>Destination URL</dt>
      <dd className="break-anywhere">{query.data.url}</dd>
      <dt>Description</dt>
      <dd>{query.data.description || "No description provided."}</dd>
      <dt>Endpoint ID</dt>
      <dd className="id-detail">
        <code>{query.data.id}</code>
        <CopyButton value={query.data.id} />
      </dd>
      <dt>Subscribed event type IDs</dt>
      <dd>
        {query.data.eventTypeIds.map((eventId) => (
          <div className="id-detail" key={eventId}>
            <code>{eventId}</code>
            <CopyButton value={eventId} />
          </div>
        ))}
      </dd>
      <dt>Configuration</dt>
      <dd>
        {query.data.configuration.enabled ? "Enabled" : "Disabled"} ·{" "}
        {query.data.configuration.rateLimitPerSecond} requests/s ·{" "}
        {query.data.configuration.concurrencyLimit} concurrent ·{" "}
        {query.data.configuration.timeoutMs} ms timeout ·{" "}
        {query.data.configuration.maxAttempts} attempts
      </dd>
      <dt>Signing secret</dt>
      <dd>
        {query.data.signingSecretVersion > 0
          ? `Version ${query.data.signingSecretVersion} · secret hidden`
          : "Not configured — generate a secret using Rotate secret."}
      </dd>
      <dt>Created</dt>
      <dd>{date(query.data.createdAt)}</dd>
    </dl>
  );
}
export default function EndpointsPage() {
  const queryClient = useQueryClient();
  const { app } = useWorkspace();
  const [page, setPage] = useState(0);
  const [create, setCreate] = useState(false);
  const [issued, setIssued] = useState<IssuedEndpoint | null>(null);
  const [configure, setConfigure] = useState<Endpoint | null>(null);
  const [rotate, setRotate] = useState<Endpoint | null>(null);
  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ["endpoints", app!.id] });
    void queryClient.invalidateQueries({ queryKey: ["endpoint", app!.id] });
  }
  const rotation = useMutation({
    mutationFn: async (endpoint: Endpoint) => {
      const issued = await endpointsApi.rotateSecret(app!.id, endpoint.id);
      setRotate(null);
      setIssued(issued);
      refresh();
    },
  });
  const [selected, setSelected] = useState<string | null>(null);
  const query = useQuery({
    queryKey: ["endpoints", app!.id, page],
    queryFn: () => endpointsApi.list(app!.id, { page, size: 20 }),
  });
  const columns: ColumnDef<Endpoint>[] = [
    {
      header: "Destination",
      accessorKey: "url",
      cell: ({ row }) => (
        <button
          className="resource-link"
          onClick={() => setSelected(row.original.id)}
        >
          <span className="table-icon">
            <Globe2 size={16} />
          </span>
          <span className="endpoint-cell">
            <strong>{row.original.url}</strong>
            <small>{row.original.description || "Webhook endpoint"}</small>
          </span>
        </button>
      ),
    },
    {
      header: "Subscriptions",
      accessorKey: "eventTypeIds",
      cell: ({ row }) => (
        <span className="badge">
          {row.original.eventTypeIds.length} event{" "}
          {row.original.eventTypeIds.length === 1 ? "type" : "types"}
        </span>
      ),
    },
    {
      header: "Created",
      accessorKey: "createdAt",
      cell: ({ getValue }) => date(getValue<string>()),
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex gap-1 flex-wrap">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSelected(row.original.id)}
          >
            View details
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setConfigure(row.original)}
          >
            Configure
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              rotation.reset();
              setRotate(row.original);
            }}
          >
            Rotate secret
          </Button>
          <DeleteResource
            name="endpoint"
            description="This destination will be removed from active use. Its configuration and subscription history are retained."
            remove={() => endpointsApi.remove(app!.id, row.original.id)}
            onDeleted={() => {
              setPage(0);
              refresh();
            }}
          />
        </div>
      ),
    },
  ];
  return (
    <>
      <PageHeader
        eyebrow={app!.name}
        title="Endpoints"
        description="Your destinations, connected to the events that matter."
        action={
          <Button onClick={() => setCreate(true)}>
            <Plus size={16} />
            Create endpoint
          </Button>
        }
      />
      <div className="subtle-note">
        <Globe2 size={17} />
        <span>
          Each endpoint subscribes to one or more event types in this
          application.
        </span>
      </div>
      {query.isPending ? (
        <Loading />
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DataTable
          data={query.data}
          columns={columns}
          page={page}
          onPage={setPage}
          emptyTitle="Add your first destination"
          emptyDescription="Connect an HTTP endpoint and choose which event types it subscribes to."
        />
      )}
      <Dialog
        open={create || !!selected || !!issued || !!configure || !!rotate}
        onOpenChange={(open) => {
          if (!open && !rotation.isPending) {
            setCreate(false);
            setSelected(null);
            setIssued(null);
            setConfigure(null);
            setRotate(null);
          }
        }}
      >
        <DialogContent>
          {issued ? (
            <>
              <DialogHeader
                title="Save signing secret"
                description="Copy this secret to your receiving application. It will not be shown again."
              />
              <div className="secret-box">
                <KeyRound size={20} />
                <code data-testid="signing-secret">{issued.signingSecret}</code>
                <CopyButton value={issued.signingSecret} label="Copy secret" />
              </div>
              <p className="field-hint">
                Signing secret version {issued.endpoint.signingSecretVersion}.
                Rotation immediately replaces the previous secret for future
                signing.
              </p>
              <div className="dialog-actions">
                <Button onClick={() => setIssued(null)}>
                  I’ve saved the secret
                </Button>
              </div>
            </>
          ) : configure ? (
            <>
              <DialogHeader
                title="Endpoint configuration"
                description="Control future delivery behavior for this endpoint."
              />
              <ConfigurationForm
                endpoint={configure}
                onSaved={() => {
                  setConfigure(null);
                  refresh();
                }}
              />
            </>
          ) : rotate ? (
            <>
              <DialogHeader
                title="Rotate signing secret?"
                description="A new secret will replace the old one. Save it and update verification in your receiving application."
              />
              <ErrorMessage error={rotation.error} />
              <div className="dialog-actions">
                <Button
                  variant="outline"
                  disabled={rotation.isPending}
                  onClick={() => setRotate(null)}
                >
                  Cancel
                </Button>
                <Button
                  disabled={rotation.isPending}
                  onClick={() => rotation.mutate(rotate)}
                >
                  Rotate signing secret
                </Button>
              </div>
            </>
          ) : create ? (
            <>
              <DialogHeader
                title="Create endpoint"
                description="Connect a destination to your application’s event types. A signing secret will be generated."
              />
              <CreateEndpoint
                onClose={() => setCreate(false)}
                onIssued={(value) => {
                  setCreate(false);
                  setIssued(value);
                }}
              />
            </>
          ) : selected ? (
            <>
              <DialogHeader
                title="Endpoint details"
                description="Destination, subscriptions, configuration, and signing-secret metadata."
              />
              <EndpointDetails id={selected} />
            </>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
