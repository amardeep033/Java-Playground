import {SubscriptionSelect} from "./subscription-select";
import {statusColumn} from "@/components/shared/column-filter";
import { ActivationToggle } from "@/components/shared/activation-toggle";
import { ListFilters } from "@/components/shared/list-filters";
import { ResourceSettings } from "@/components/shared/resource-settings";
import { useDebounced } from "@/features/activity/shared";
import { useState, useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import { Globe2, Plus, KeyRound, Pencil } from "lucide-react";
import { endpointsApi } from "./api";
import { endpointSchema, type EndpointValues } from "./schemas";
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
import { DeleteResource } from "@/components/shared/delete-resource";
import { RoutingEditor } from "./routing-editor";
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
  const { app } = useWorkspace();
  const queryClient = useQueryClient();
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
        id="endpoint-name"
        label="Endpoint name"
        error={errors.name?.message}
      >
        <Input
          id="endpoint-name"
          placeholder="Order processing webhook"
          {...register("name")}
        />
      </Field>
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
      <Field id="create-subscriptions" label="Event subscriptions" error={errors.eventTypeIds?.message}>
        <SubscriptionSelect id="create-subscriptions" appId={app!.id} selected={selected} onChange={ids=>setValue("eventTypeIds",ids,{shouldValidate:true})}/>
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button type="button" variant="outline" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={mutation.isPending || !selected.length}>
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
      <dt>Name</dt>
      <dd>{query.data.name}</dd>
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
        {query.data.active ? "Active" : "Inactive"} ·{" "}
        {query.data.configuration.rateLimitPerSecond} requests/s ·{" "}
        {query.data.configuration.concurrencyLimit} concurrent ·{" "}
        {query.data.configuration.timeoutMs} ms timeout ·{" "}
        {query.data.configuration.maxAttempts} attempts
      </dd>
      <dt>Signing secret</dt>
      <dd>
        {query.data.signingSecretVersion > 0
          ? `Rotation ${query.data.signingSecretVersion} · secret hidden`
          : "Not configured — generate a secret using Rotate secret."}
      </dd>
      <dt>Created</dt>
      <dd>{date(query.data.createdAt)}</dd>
    </dl>
  );
}
export default function EndpointsPage() {
  const queryClient = useQueryClient();
  const { app, canAdmin } = useWorkspace();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const [status, setStatus] = useState("");

  const [create, setCreate] = useState(false);
  const [issued, setIssued] = useState<IssuedEndpoint | null>(null);
  const [editing, setEditing] = useState<Endpoint | null>(null);
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
    placeholderData:previous=>previous,
    queryKey: ["endpoints", app!.id, page, q, status],
    queryFn: () => endpointsApi.list(app!.id, { page, size: 20, q, status }),
  });
  const columns = useMemo<ColumnDef<Endpoint>[]>(
    () => [
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
              <strong>{row.original.name}</strong>
              <small>{row.original.url}</small>
              {row.original.description && <small>{row.original.description}</small>}
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
        cell: ({ row }) => <Button variant="ghost" size="icon" aria-label="Edit" title={`Edit ${row.original.name}`} onClick={()=>setEditing(row.original)}><Pencil size={16} /></Button>,
      },
    ],
    [],
  );
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
      <ListFilters hideStatus
        label="endpoints"
        search={search}
        onSearch={(value) => {
          setSearch(value);
          setPage(0);
        }}
        status={status}
        onStatus={(value) => {
          setStatus(value);
          setPage(0);
        }}
        options={[
          { value: "ACTIVE", label: "Active" },
          { value: "INACTIVE", label: "Inactive" },
        ]}
      />
      {query.isPending ? (
        <Loading />
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DataTable
          data={query.data}
          columns={columns}
          columnFilters={{status:statusColumn(status,value=>{setStatus(value);setPage(0);},undefined)}}
          page={page}
          onPage={setPage}
          emptyTitle={
            q || status ? "No matching endpoints" : "Add your first destination"
          }
          emptyDescription="Connect an HTTP endpoint and choose which event types it subscribes to."
        />
      )}

      {/* Keep the editor outside table rows so background refreshes preserve drafts. */}
      {editing && <ResourceSettings showTrigger={false} open onOpenChange={open=>{if(!open)setEditing(null);}} name={editing.name}
        metadata={{title:"Edit endpoint",name:editing.name,description:editing.description,editName:true,editDescription:true,
          save:body=>endpointsApi.update(app!.id,editing.id,body),onSaved:refresh}}>
        {close=><>
          <ActivationToggle canChange={canAdmin} active={(query.data?.items.find(endpoint=>endpoint.id===editing.id) ?? editing).active} name={editing.url} save={async active=>{await endpointsApi.setActive(app!.id,editing.id,active);setEditing(current=>current ? {...current,active} : null);}} />
          <section className="editor-section"><h3>Delivery configuration</h3><ConfigurationForm endpoint={editing} onSaved={()=>{setEditing(null);refresh();}}/></section>
          {canAdmin&&<section className="editor-section"><h3>Destination & subscriptions</h3><RoutingEditor endpoint={editing} onSaved={()=>{setEditing(null);refresh();}}/></section>}
          <div className="settings-action-grid">
            <Button variant="outline" onClick={()=>{close();rotation.reset();setRotate(editing);}}>Rotate secret</Button>
          </div>
          {canAdmin && <div className="danger-zone"><h3>Delete endpoint</h3><p>Stops new deliveries to this destination. History is retained.</p><DeleteResource name="endpoint" description="This destination will be removed from active use. History is retained; this cannot be undone in the UI." remove={()=>endpointsApi.remove(app!.id,editing.id)} onDeleted={()=>{close();setPage(0);refresh();}} /></div>}
        </>}
      </ResourceSettings>}
      <Dialog
        open={create || !!selected || !!issued || !!rotate}
        onOpenChange={(open) => {
          if (!open && !rotation.isPending) {
            setCreate(false);
            setSelected(null);
            setIssued(null);
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
                Signing secret rotation {issued.endpoint.signingSecretVersion}.
                Rotation immediately replaces the previous secret for future
                signing.
              </p>
              <div className="dialog-actions">
                <Button onClick={() => setIssued(null)}>
                  I’ve saved the secret
                </Button>
              </div>
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
