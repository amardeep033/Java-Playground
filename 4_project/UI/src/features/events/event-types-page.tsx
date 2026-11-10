import {statusColumn} from "@/components/shared/column-filter";
import { ActivationToggle } from "@/components/shared/activation-toggle";
import { ListFilters } from "@/components/shared/list-filters";
import { ResourceSettings } from "@/components/shared/resource-settings";
import { useDebounced } from "@/features/activity/shared";
import { useState, useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import { Braces, Plus } from "lucide-react";
import { eventTypesApi } from "./api";
import type { EventType } from "@/types/api";
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
import { date } from "@/lib/utils";
const schema = z.object({
  name: z
    .string()
    .min(1, "Enter an event type name.")
    .max(120)
    .regex(
      /^[a-zA-Z0-9_.-]+$/,
      "Use letters, numbers, dots, underscores, or hyphens.",
    ),
  description: z.string().max(500).optional(),
});
function CreateEventType({ onClose }: { onClose: () => void }) {
  const { app } = useWorkspace();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });
  const mutation = useMutation({
    mutationFn: (values: z.infer<typeof schema>) =>
      eventTypesApi.create(app!.id, values),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["event-types", app!.id],
      });
      onClose();
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <Field
        id="event-name"
        label="Event type name"
        hint="Permanent routing contract: this name cannot be changed after creation. Create a new event type to use a different name."
        error={errors.name?.message}
      >
        <Input
          id="event-name"
          placeholder="order.created"
          autoFocus
          {...register("name")}
        />
      </Field>
      <Field
        id="event-description"
        label="Description (optional)"
        error={errors.description?.message}
      >
        <Textarea
          id="event-description"
          placeholder="When a new order is placed…"
          {...register("description")}
        />
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button variant="outline" type="button" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={mutation.isPending}>Create event type</Button>
      </div>
    </form>
  );
}
function EventDetails({ id }: { id: string }) {
  const { app } = useWorkspace();
  const query = useQuery({
    queryKey: ["event-type", app!.id, id],
    queryFn: () => eventTypesApi.get(app!.id, id),
  });
  return query.isPending ? (
    <Loading />
  ) : query.isError ? (
    <ErrorMessage error={query.error} retry={() => void query.refetch()} />
  ) : (
    <dl className="detail-list">
      <dt>Name</dt>
      <dd>
        <code>{query.data.name}</code>
      </dd>
      <dt>Description</dt>
      <dd>{query.data.description || "No description provided."}</dd>
      <dt>Event type ID</dt>
      <dd className="id-detail">
        <code>{query.data.id}</code>
        <CopyButton value={query.data.id} />
      </dd>
      <dt>Status</dt>
      <dd>{query.data.active ? "Active" : "Inactive"}</dd>
      <dt>Created</dt>
      <dd>{date(query.data.createdAt)}</dd>
    </dl>
  );
}
export default function EventTypesPage() {
  const queryClient = useQueryClient();
  const { app, canAdmin } = useWorkspace();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const [status, setStatus] = useState("");

  const [create, setCreate] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const query = useQuery({
    placeholderData:previous=>previous,
    queryKey: ["event-types", app!.id, page, q, status],
    queryFn: () => eventTypesApi.list(app!.id, { page, size: 20, q, status }),
  });
  const columns = useMemo<ColumnDef<EventType>[]>(
    () => [
      {
        header: "Event type",
        accessorKey: "name",
        cell: ({ row }) => (
          <button
            className="resource-link"
            onClick={() => setSelected(row.original.id)}
          >
            <span className="table-icon">
              <Braces size={16} />
            </span>
            <code>{row.original.name}</code>
          </button>
        ),
      },
      {
        header: "Description",
        accessorKey: "description",
        cell: ({ getValue }) => (
          <span className="muted">{getValue<string>() || "—"}</span>
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
        cell: ({ row }) => <ResourceSettings name={row.original.name} metadata={{title:"Edit event type",description:row.original.description,editDescription:true,
          save:body=>eventTypesApi.update(app!.id,row.original.id,{description:body.description}),onSaved:()=>{void queryClient.invalidateQueries({queryKey:["event-types",app!.id]});void queryClient.invalidateQueries({queryKey:["event-type",app!.id]});}}}>
          <ActivationToggle canChange={canAdmin} active={row.original.active} name={row.original.name} save={active=>eventTypesApi.setActive(app!.id,row.original.id,active)} />
          {canAdmin && <div className="danger-zone"><h3>Delete event type</h3><p>Stops future events of this type. Existing event history remains.</p><DeleteResource name={row.original.name} description="Future subscriptions will no longer include this event type. History is retained; this cannot be undone in the UI." remove={()=>eventTypesApi.remove(app!.id,row.original.id)} onDeleted={()=>{setPage(0);void queryClient.invalidateQueries();}} /></div>}
        </ResourceSettings>,
      },
    ],
    [app!.id, canAdmin, queryClient],
  );
  return (
    <>
      <PageHeader
        eyebrow={app!.name}
        title="Event types"
        description="A shared vocabulary for what happens in your application."
        action={
          <Button onClick={() => setCreate(true)}>
            <Plus size={16} />
            Create event type
          </Button>
        }
      />
      <ListFilters hideStatus
        label="event types"
        status={status}
        onStatus={(value) => {
          setStatus(value);
          setPage(0);
        }}
        options={[
          { value: "ACTIVE", label: "Active" },
          { value: "INACTIVE", label: "Inactive" },
        ]}
        search={search}
        onSearch={(value) => {
          setSearch(value);
          setPage(0);
        }}
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
            q || status ? "No matching event types" : "Give your events a name"
          }
          emptyDescription="Create an event type like order.created, then subscribe an endpoint to it."
        />
      )}

      <Dialog open={create} onOpenChange={setCreate}>
        <DialogContent>
          <DialogHeader
            title="Create event type"
            description="Define an event your application can use."
          />
          {create && <CreateEventType onClose={() => setCreate(false)} />}
        </DialogContent>
      </Dialog>
      <Dialog
        open={!!selected}
        onOpenChange={(open) => {
          if (!open) setSelected(null);
        }}
      >
        <DialogContent>
          <DialogHeader
            title="Event type details"
            description="The definition registered with this application."
          />
          {selected && <EventDetails id={selected} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
