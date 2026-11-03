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
});
function CreateApplication({ onClose }: { onClose: () => void }) {
  const { user } = useWorkspace();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });
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
  const { user } = useWorkspace();
  const [page, setPage] = useState(0);
  const [create, setCreate] = useState(false);
  const query = useQuery({
    queryKey: ["applications", user.id, page],
    queryFn: () => applicationsApi.list(user.id, { page, size: 12 }),
  });
  return (
    <>
      <PageHeader
        title="Applications"
        description="The starting point for your webhook integrations."
        action={
          <Button onClick={() => setCreate(true)}>
            <Plus size={16} />
            Create application
          </Button>
        }
      />
      <div className="context-banner">
        <span className="context-icon">
          <Box size={20} />
        </span>
        <div>
          <strong>A separate space for every integration.</strong>
          <p>
            Each application brings its own event types, destinations, and API
            keys together.
          </p>
        </div>
      </div>
      <div className="section-heading">
        <h2>
          Your applications{" "}
          {query.data && (
            <span className="count-badge">{query.data.totalElements}</span>
          )}
        </h2>
        <span>Organized. Connected. Ready to build.</span>
      </div>
      {query.isPending ? (
        <Loading />
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : query.data.totalElements === 0 ? (
        <EmptyState
          title="Your first connection starts here"
          description="Create an application, define an event type, and add your first destination."
          action={
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
                <div className="app-card-top">
                  <span className="app-icon">
                    <Box size={22} />
                  </span>
                  <DeleteResource
                    name={app.name}
                    description="This will remove the application and all of its endpoints, event types, and keys from the workspace. Records are retained for history."
                    remove={() => applicationsApi.remove(user.id, app.id)}
                    onDeleted={() => {
                      setPage(0);
                      void queryClient.invalidateQueries({
                        queryKey: ["applications", user.id],
                      });
                    }}
                  />
                </div>
                <h3>
                  <Link to={`/users/${user.id}/apps/${app.id}/overview`}>
                    {app.name}
                    <ArrowRight size={18} />
                  </Link>
                </h3>
                <div className="app-id">
                  <code>{app.id.slice(0, 18)}…</code>
                  <CopyButton value={app.id} />
                </div>
                <div className="app-card-footer">
                  <span>Created {date(app.createdAt)}</span>
                  <Link to={`/users/${user.id}/apps/${app.id}/overview`}>
                    Open application
                    <ArrowRight size={14} />
                  </Link>
                </div>
              </article>
            ))}
          </div>
          <div className="pagination">
            <span>
              Page {page + 1} of {query.data.totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= query.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </Button>
          </div>
        </>
      )}
      <Dialog open={create} onOpenChange={setCreate}>
        <DialogContent>
          <DialogHeader
            title="Create application"
            description="Give your webhook integration a space of its own."
          />
          {create && <CreateApplication onClose={() => setCreate(false)} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
