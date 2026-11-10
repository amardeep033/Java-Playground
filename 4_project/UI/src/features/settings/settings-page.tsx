import { useState } from "react";
import { ListFilters } from "@/components/shared/list-filters";
import { ResourceSettings } from "@/components/shared/resource-settings";
import { useDebounced } from "@/features/activity/shared";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { DeleteResource } from "@/components/shared/delete-resource";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { UserRound } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { passwordSchema, type PasswordValues } from "@/features/auth/schemas";
import { authApi, usersApi } from "@/features/auth/api";
import { PageHeader } from "@/components/shared/page-header";
import { CopyButton, ErrorMessage } from "@/components/shared/feedback";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { Timestamp } from "@/features/activity/shared";
import { date, initials } from "@/lib/utils";
export default function SettingsPage({overlay=false}:{overlay?:boolean}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, canManageUsers } = useWorkspace();
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const attempts = useQuery({
    queryKey: ["login-attempts", user.id, q],
    queryFn: () => authApi.attempts(q),
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema) });
  const mutation = useMutation({
    mutationFn: (values: PasswordValues) =>
      usersApi.changePassword(user.id, values),
    onSuccess: () => {
      reset();
      queryClient.clear();
      navigate("/", {
        replace: true,
      });
    },
  });
  return (
    <>
      {!overlay && <PageHeader
        title="Profile & security"
        description="Your workspace identity, credentials, and preferences."
      />}
      <div className="settings-stack">
        <section className="panel">
          <div className="panel-heading">
            <h2>
              <UserRound size={17} />
              Profile
            </h2>
            <div className="profile-controls">
              <span className="badge">Registered {date(user.createdAt)}</span>
              <ResourceSettings
                name={user.name}
                metadata={{
                  title: "Edit profile",
                  name: user.name,
                  editName: true,
                  save: (body) => usersApi.update(user.id, { name: body.name! }),
                  onSaved: () => {
                    void queryClient.invalidateQueries({ queryKey: ["user", user.id] });
                    void queryClient.invalidateQueries({ queryKey: ["session"] });
                  },
                }}
              >
                <>
                  <section className="editor-section"><h3>Change password</h3><p className="muted">Changing your password signs you out and prevents renewal on your other devices.</p>
          <form
            className="form-stack password-form"
            onSubmit={handleSubmit((values) => {
              mutation.mutate(values);
            })}
          >
            <Field
              id="current-password"
              label="Current password"
              error={errors.currentPassword?.message}
            >
              <Input
                id="current-password"
                type="password"
                autoComplete="current-password"
                {...register("currentPassword")}
              />
            </Field>
            <div className="form-columns">
              <Field
                id="new-password"
                label="New password"
                error={errors.newPassword?.message}
                hint="Use 12–128 characters."
              >
                <Input
                  id="new-password"
                  type="password"
                  autoComplete="new-password"
                  {...register("newPassword")}
                />
              </Field>
              <Field
                id="confirm-password"
                label="Confirm new password"
                error={errors.confirmation?.message}
              >
                <Input
                  id="confirm-password"
                  type="password"
                  autoComplete="new-password"
                  {...register("confirmation")}
                />
              </Field>
            </div>
            <ErrorMessage error={mutation.error} />
            <div className="dialog-actions">
              <Button disabled={mutation.isPending}>Update password</Button>
            </div>
          </form>
                  </section>
                  <div className="danger-zone">
                    <h3>Delete account</h3>
                    <p>Permanently removes your account from the UI. Company applications remain. Keep at least one company ADMIN.</p>
                    <DeleteResource
                      name="account"
                      label="Delete account"
                      description="Your account access will be removed. Company applications remain. Keep at least one company ADMIN. History is retained. This cannot be undone in the UI."
                      remove={() => usersApi.remove(user.id)}
                      onDeleted={() => {
                        queryClient.clear();
                        navigate("/");
                      }}
                    />
                  </div>
                </>
              </ResourceSettings>
            </div>
          </div>
          <div className="profile-summary">
            <span className="profile-avatar">{initials(user.name)}</span>
            <div>
              <h3>{user.name}</h3>
              <p>{user.email}</p>
            </div>
          </div>
          <dl className="profile-id">
            <dt>
              Your user ID{" "}
              <span className="muted">· Your account identifier</span>
            </dt>
            <dd className="id-detail">
              <code>{user.id}</code>
              <CopyButton value={user.id} />
            </dd>
          </dl>
          <div className="profile-access">
            <div className="panel-heading">
              <h3>Account access</h3>
              <span className="badge">{user.role}</span>
            </div>
            <p className="muted">
              {canManageUsers ? "Company administrator: manage your company’s users and applications." : "Access the applications assigned to you by your company administrator."}
            </p>
            <p className="muted">
              Your login renews automatically for up to 30 days. Signing out ends renewal.
            </p>
          </div>
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>Recent login attempts</h2>
            <Button variant="ghost" onClick={() => void attempts.refetch()}>
              Refresh history
            </Button>
          </div>
          <p className="muted">
            Last 20 attempts. Five failed passwords within 15 minutes lock login
            for 15 minutes.
          </p>
          <ListFilters
            label="login attempts"
            search={search}
            onSearch={setSearch}
          />
          <ErrorMessage
            error={attempts.error}
            retry={() => void attempts.refetch()}
          />
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Result</th>
                  <th>IP address</th>
                </tr>
              </thead>
              <tbody>
                {attempts.data?.map((attempt, index) => (
                  <tr key={`${attempt.attempted_at}-${index}`}>
                    <td>
                      <Timestamp value={attempt.attempted_at} />
                    </td>
                    <td>
                      {attempt.success
                        ? "Successful"
                        : attempt.reason === "LOCKED"
                          ? "Login locked"
                          : attempt.reason === "ROLE_DENIED"
                            ? "Admin access denied"
                            : "Wrong email or password"}
                    </td>
                    <td>
                      <code>{attempt.ip_address}</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>

    </>
  );
}
