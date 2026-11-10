import { ActivationToggle } from "@/components/shared/activation-toggle";
import { ListFilters } from "@/components/shared/list-filters";
import { ResourceSettings } from "@/components/shared/resource-settings";
import { Pagination } from "@/components/shared/pagination";
import { useDebounced } from "@/features/activity/shared";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Box, Plus } from "lucide-react";
import { applicationsApi } from "./api";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import {
  CopyButton,
  EmptyState,
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { Field, Input } from "@/components/ui/field";
import { DeleteResource } from "@/components/shared/delete-resource";
import { date } from "@/lib/utils";
const schema = z.object({
  name: z.string().trim().min(1, "Give your application a name.").max(120),
  clientId: z.string().uuid("Select a client."),
});
function CreateApplication({ onClose }: { onClose: () => void }) {
  const { user } = useWorkspace();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), defaultValues: {clientId:user.clientId} });
  const mutation = useMutation({
    mutationFn: (value: z.infer<typeof schema>) =>
      applicationsApi.create(user.id, value),
    onSuccess: (app) => {
      void queryClient.invalidateQueries({
        queryKey: ["applications", user.id],
      });
      onClose();
      navigate(`/users/${user.id}/apps/${app.id}/overview`);
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((value) => mutation.mutate(value))}
    >
      <Field
        id="application-name"
        label="Application name"
        error={errors.name?.message}
        hint="Choose a name your team will recognize."
      >
        <Input
          id="application-name"
          placeholder="e.g. Acme production"
          autoFocus
          {...register("name")}
        />
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button variant="outline" type="button" onClick={onClose}>
          Cancel
        </Button>
        <Button disabled={mutation.isPending}>
          Create application
          <ArrowRight size={15} />
        </Button>
      </div>
    </form>
  );
}
export default function ApplicationsPage() {
  const queryClient = useQueryClient();
  const { user, canCreateApplications } = useWorkspace();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const [status, setStatus] = useState("");

  const [create, setCreate] = useState(false);
  const query = useQuery({
    queryKey: ["applications", user.id, page, q, status],
    queryFn: () => applicationsApi.list(user.id, { page, size: 12, q, status }),
  });
  return (
    <>
      <PageHeader
        title="Applications"
        description="Manage your webhook integrations."
        action={canCreateApplications &&
          <Button onClick={() => setCreate(true)}>
            <Plus size={16} />
            Create application
          </Button>
        }
      />
      <ListFilters
        label="applications"
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
      ) : query.data.totalElements === 0 ? (
        <EmptyState
          title={
            q || status
              ? "No matching applications"
              : canCreateApplications ? "Your first connection starts here" : "No applications assigned"
          }
          description={
            q || status
              ? "Try another name, description, or ID."
              : canCreateApplications ? "Create an application, define an event type, and add your first destination." : "Your client administrator can grant you access to an application."
          }
          action={canCreateApplications &&
            <Button onClick={() => setCreate(true)}>
              <Plus size={16} />
              Create application
            </Button>
          }
        />
      ) : (
        <>
          <div className="application-grid">
            {query.data.items.map((app) => (
              <article className="application-card" key={app.id}>
                <span className="app-icon"><Box size={22} /></span>
                <div className="application-summary"><h3><Link to={`/users/${user.id}/apps/${app.id}/overview`}>{app.name}</Link></h3>{app.description && <p className="muted">{app.description}</p>}</div>
                <div className="app-id"><code>{app.id.slice(0,18)}…</code><CopyButton value={app.id} /></div>
                <span className="app-created">Created {date(app.createdAt)}</span>
                {app.clientRole==="ADMIN" && <ResourceSettings name={app.name} metadata={{title:"Edit application",name:app.name,description:app.description,editName:true,editDescription:true,
                  save:body=>applicationsApi.update(user.id,app.id,{name:body.name!,description:body.description}),
                  onSaved:()=>{void queryClient.invalidateQueries({queryKey:["applications",user.id]});void queryClient.invalidateQueries({queryKey:["application",user.id,app.id]});}}}>
                  <ActivationToggle canChange={app.clientRole==="ADMIN"} active={app.active} name={app.name} save={active=>applicationsApi.update(user.id,app.id,{active})} />
                  {(app.clientRole==="ADMIN") && <div className="danger-zone"><h3>Delete application</h3><p>Remove this application from active use. History is retained.</p><DeleteResource name={app.name} description="This removes the application, endpoints, event types and keys from the workspace. History is retained; this cannot be undone in the UI." remove={()=>applicationsApi.remove(user.id,app.id)} onDeleted={()=>{setPage(0);void queryClient.invalidateQueries();}} /></div>}
                </ResourceSettings>}
              </article>
            ))}
          </div>
        </>
      )}
      {query.data && <Pagination page={page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} onPage={setPage} />}
      <Dialog open={create} onOpenChange={setCreate}>
        <DialogContent>
          <DialogHeader
            title="Create application"
            description="Give your webhook integration a space of its own."
          />
          {create && canCreateApplications && <CreateApplication onClose={() => setCreate(false)} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
