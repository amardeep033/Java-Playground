import {statusColumn} from "@/components/shared/column-filter";
import { ListFilters } from "@/components/shared/list-filters";
import { ResourceSettings } from "@/components/shared/resource-settings";
import { useDebounced } from "@/features/activity/shared";
import { useState, useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import { AlertTriangle, KeyRound, Plus, RotateCw, Trash2, Pencil } from "lucide-react";
import { keysApi } from "./api";
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
import { Field, Input } from "@/components/ui/field";
import type { ApiKey, IssuedApiKey } from "@/types/api";
import { date } from "@/lib/utils";
const schema = z.object({
  name: z.string().trim().min(1, "Give your key a recognizable name.").max(120),
});
function CreateKey({
  onIssued,
  onClose,
}: {
  onIssued: (issued: IssuedApiKey) => void;
  onClose: () => void;
}) {
  const { app } = useWorkspace();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });
  // Return void: the one-time secret must never enter the mutation cache.
  const mutation = useMutation({
    mutationFn: async (value: z.infer<typeof schema>) => {
      const issued = await keysApi.create(app!.id, value);
      onIssued(issued);
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <Field
        id="key-name"
        label="Key name"
        error={errors.name?.message}
        hint="Use a name that describes where you’ll use this key."
      >
        <Input
          id="key-name"
          placeholder="e.g. Production server"
          autoFocus
          {...register("name")}
        />
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button variant="outline" type="button" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={mutation.isPending}>Generate key</Button>
      </div>
    </form>
  );
}
function KeyDetails({ id }: { id: string }) {
  const { app } = useWorkspace();
  const query = useQuery({
    queryKey: ["api-key", app!.id, id],
    queryFn: () => keysApi.get(app!.id, id),
  });
  return query.isPending ? (
    <Loading />
  ) : query.isError ? (
    <ErrorMessage error={query.error} retry={() => void query.refetch()} />
  ) : (
    <dl className="detail-list">
      <dt>Name</dt>
      <dd>{query.data.name}</dd>
      <dt>Prefix</dt>
      <dd>
        <code>{query.data.prefix}••••••</code>
      </dd>
      <dt>Key ID</dt>
      <dd className="id-detail">
        <code>{query.data.id}</code>
        <CopyButton value={query.data.id} />
      </dd>
      <dt>Created</dt>
      <dd>{date(query.data.createdAt)}</dd>
      <dt>Status</dt>
      <dd>
        <span
          className={`badge ${query.data.revokedAt ? "" : "badge-success"}`}
        >
          {query.data.revokedAt ? "Revoked" : "Active"}
        </span>
      </dd>
      {query.data.revokedAt && (
        <>
          <dt>Revoked</dt>
          <dd>{date(query.data.revokedAt)}</dd>
        </>
      )}
      {query.data.replacedById && (
        <>
          <dt>Replacement key ID</dt>
          <dd className="id-detail">
            <code>{query.data.replacedById}</code>
            <CopyButton value={query.data.replacedById} />
          </dd>
        </>
      )}
    </dl>
  );
}
export default function ApiKeysPage() {
  const { app, canAdmin } = useWorkspace();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const [status, setStatus] = useState("");

  const [create, setCreate] = useState(false);
  const [issued, setIssued] = useState<IssuedApiKey | null>(null);
  const [editing, setEditing] = useState<ApiKey | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [action, setAction] = useState<{
    type: "rotate" | "revoke";
    key: ApiKey;
  } | null>(null);
  const query = useQuery({
    placeholderData:previous=>previous,
    queryKey: ["api-keys", app!.id, page, q, status],
    queryFn: () => keysApi.list(app!.id, { page, size: 20, q, status }),
  });
  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ["api-keys", app!.id] });
    void queryClient.invalidateQueries({ queryKey: ["api-key", app!.id] });
  }
  function showIssued(value: IssuedApiKey) {
    setCreate(false);
    setIssued(value);
    refresh();
  }
  const mutation = useMutation({
    mutationFn: async (value: NonNullable<typeof action>) => {
      if (value.type === "rotate") {
        const replacement = await keysApi.rotate(app!.id, value.key.id);
        setIssued(replacement);
      } else await keysApi.revoke(app!.id, value.key.id);
      // Only the local, dismissible dialog owns secrets. Never return them here.
    },
    onSuccess: () => {
      setAction(null);
      refresh();
    },
  });
  const columns = useMemo<ColumnDef<ApiKey>[]>(
    () => [
      {
        header: "Name",
        accessorKey: "name",
        cell: ({ row }) => (
          <button
            className="resource-link"
            onClick={() => setSelected(row.original.id)}
          >
            <span className="table-icon">
              <KeyRound size={16} />
            </span>
            <strong>{row.original.name}</strong>
          </button>
        ),
      },
      {
        header: "Key prefix",
        accessorKey: "prefix",
        cell: ({ getValue }) => (
          <code className="muted">{getValue<string>()}••••••</code>
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
        title="API keys"
        description="Credentials for your application. Always under your control."
        action={
          <Button onClick={() => setCreate(true)}>
            <Plus size={16} />
            Generate API key
          </Button>
        }
      />
      <ListFilters hideStatus
        label="API keys"
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
          { value: "REVOKED", label: "Revoked" },
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
          columnFilters={{status:statusColumn(status,value=>{setStatus(value);setPage(0);},[{value:"ACTIVE",label:"Active"},{value:"REVOKED",label:"Revoked"}])}}
          page={page}
          onPage={setPage}
          emptyTitle={
            q || status ? "No matching api keys" : "Create your first API key"
          }
          emptyDescription="Generate an application credential, then save the secret in your secret manager."
        />
      )}

      {/* Editor lifetime is independent of delayed search results and table refreshes. */}
      {editing && <ResourceSettings showTrigger={false} open onOpenChange={open=>{if(!open)setEditing(null);}} name={editing.name}
        metadata={{title:"Edit API key",name:editing.name,editName:true,
          save:body=>keysApi.update(app!.id,editing.id,{name:body.name!}),onSaved:refresh}}>
        {close=><>
          <div className="editor-activation"><span>{editing.revokedAt?"Revoked":"Active"}</span><button type="button" role="switch" className="activation-switch" aria-label="API key active" aria-checked={!editing.revokedAt} disabled={!canAdmin||!!editing.revokedAt} title={editing.revokedAt?"Revoked keys cannot be reactivated":"Revoke key (requires confirmation)"} onClick={()=>{close();mutation.reset();setAction({type:"revoke",key:editing});}}><span/></button></div>
          <Button variant="outline" disabled={!!editing.revokedAt} onClick={()=>{close();mutation.reset();setAction({type:"rotate",key:editing});}}><RotateCw size={14} />Rotate</Button>
          {canAdmin && <div className="danger-zone"><h3>Revoke API key</h3><p>Rejects future requests using this key. Accepted deliveries continue.</p><Button variant="ghost" className="danger-link" disabled={!!editing.revokedAt} onClick={()=>{close();mutation.reset();setAction({type:"revoke",key:editing});}}><Trash2 size={14} />Revoke</Button></div>}
        </>}
      </ResourceSettings>}
      <Dialog
        open={create || !!issued || !!action || !!selected}
        onOpenChange={(open) => {
          if (!open && !mutation.isPending) {
            setCreate(false);
            setIssued(null);
            setAction(null);
            setSelected(null);
          }
        }}
      >
        <DialogContent>
          {issued ? (
            <>
              <DialogHeader
                title="Save your API key"
                description="This is the only time you’ll see this secret. Copy it before closing."
              />
              <div className="secret-box">
                <KeyRound size={20} />
                <code data-testid="one-time-secret">{issued.secret}</code>
                <CopyButton value={issued.secret} label="Copy secret" />
              </div>
              <div className="secret-caption">
                <AlertTriangle size={15} />
                Closing this dialog permanently hides the secret.
              </div>
              <div className="dialog-actions">
                <Button onClick={() => setIssued(null)}>
                  I’ve saved my key
                </Button>
              </div>
            </>
          ) : action ? (
            <>
              <DialogHeader
                title={
                  action.type === "rotate"
                    ? "Rotate API key?"
                    : "Revoke API key?"
                }
                description={
                  action.type === "rotate"
                    ? `“${action.key.name}” will be revoked immediately and replaced with a new key. Update any clients using the old key.`
                    : `“${action.key.name}” will be revoked immediately. This action cannot be undone.`
                }
              />
              <ErrorMessage error={mutation.error} />
              <div className="dialog-actions">
                <Button
                  variant="outline"
                  disabled={mutation.isPending}
                  onClick={() => setAction(null)}
                >
                  Cancel
                </Button>
                <Button
                  variant={action.type === "revoke" ? "destructive" : "default"}
                  disabled={mutation.isPending}
                  onClick={() => mutation.mutate(action)}
                >
                  {action.type === "rotate" ? "Rotate key" : "Revoke key"}
                </Button>
              </div>
            </>
          ) : create ? (
            <>
              <DialogHeader
                title="Generate API key"
                description="Create a new credential for this application."
              />
              <CreateKey
                onIssued={showIssued}
                onClose={() => setCreate(false)}
              />
            </>
          ) : selected ? (
            <>
              <DialogHeader
                title="API key details"
                description="Metadata and lifecycle history. Secrets cannot be retrieved."
              />
              <KeyDetails id={selected} />
            </>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
