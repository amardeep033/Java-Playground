import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ColumnDef } from "@tanstack/react-table";
import {
  AlertTriangle,
  KeyRound,
  Plus,
  RotateCw,
  ShieldCheck,
  Trash2,
} from "lucide-react";
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
  const { app } = useWorkspace();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [create, setCreate] = useState(false);
  const [issued, setIssued] = useState<IssuedApiKey | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [action, setAction] = useState<{
    type: "rotate" | "revoke";
    key: ApiKey;
  } | null>(null);
  const query = useQuery({
    queryKey: ["api-keys", app!.id, page],
    queryFn: () => keysApi.list(app!.id, { page, size: 20 }),
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
  const columns: ColumnDef<ApiKey>[] = [
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
      header: "Status",
      accessorKey: "revokedAt",
      cell: ({ row }) => (
        <span
          className={`badge ${row.original.revokedAt ? "" : "badge-success"}`}
        >
          <span className="badge-dot" />
          {row.original.revokedAt ? "Revoked" : "Active"}
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
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="sm"
            disabled={!!row.original.revokedAt}
            onClick={() => {
              mutation.reset();
              setAction({ type: "rotate", key: row.original });
            }}
          >
            <RotateCw size={13} />
            Rotate
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={!!row.original.revokedAt}
            onClick={() => {
              mutation.reset();
              setAction({ type: "revoke", key: row.original });
            }}
          >
            <Trash2 size={13} />
            Revoke
          </Button>
        </div>
      ),
    },
  ];
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
      <div className="subtle-note">
        <ShieldCheck size={17} />
        <span>
          Key secrets are shown once. Store them somewhere safe before closing
          the dialog.
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
          emptyTitle="Create your first API key"
          emptyDescription="Generate an application credential, then save the secret in your secret manager."
        />
      )}
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
