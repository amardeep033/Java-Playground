import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { DeleteResource } from "@/components/shared/delete-resource";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole, Moon, Sun, UserRound } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { useTheme } from "@/app/providers/app-providers";
import { passwordSchema, type PasswordValues } from "@/features/auth/schemas";
import { authApi, usersApi } from "@/features/auth/api";
import { PageHeader } from "@/components/shared/page-header";
import { CopyButton, ErrorMessage } from "@/components/shared/feedback";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/field";
import { Timestamp } from "@/features/activity/shared";
import { date, initials } from "@/lib/utils";
export default function SettingsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useWorkspace();
  const { dark, toggle } = useTheme();
  const session = useQuery({ queryKey: ["session"], queryFn: authApi.me });
  const attempts = useQuery({
    queryKey: ["login-attempts", user.id],
    queryFn: authApi.attempts,
  });
  const logout = useMutation({
    mutationFn: () => authApi.logout(true),
    onSuccess: async () => {
      await queryClient.cancelQueries();
      queryClient.clear();
      navigate("/", { replace: true });
    },
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
      navigate("/", { replace: true });
    },
  });
  return (
    <>
      <PageHeader
        title="Profile & security"
        description="Your workspace identity, credentials, and preferences."
      />
      <div className="settings-stack">
        <section className="panel appearance-panel">
          <div>
            <h2>Delete account</h2>
            <p className="muted">
              Removes your workspace and all applications from active use.
            </p>
          </div>
          <DeleteResource
            name="account"
            label="Delete account"
            description="Your account, applications, endpoints, event types and keys will be soft-deleted. Their history is retained. This cannot be undone in the UI."
            remove={() => usersApi.remove(user.id)}
            onDeleted={() => {
              queryClient.clear();
              navigate("/");
            }}
          />
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>
              <UserRound size={17} />
              Profile
            </h2>
            <span className="badge">Registered {date(user.createdAt)}</span>
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
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>
              <LockKeyhole size={17} />
              Change password
            </h2>
          </div>
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
            <div>
              <Button disabled={mutation.isPending}>Update password</Button>
            </div>
          </form>
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>Session & access</h2>
            <span className="badge">{session.data?.role || "USER"}</span>
          </div>
          <p className="muted">
            {session.data?.role === "ADMIN"
              ? "Administrator: manage applications across workspaces."
              : "User: manage only your own workspace and applications."}
          </p>
          <p className="muted">
            Session expires{" "}
            {session.data ? <Timestamp value={session.data.expiresAt} /> : "—"}.
            Password changes sign out all sessions.
          </p>
          <ErrorMessage error={logout.error} />
          <Button
            variant="outline"
            disabled={logout.isPending}
            onClick={() => logout.mutate()}
          >
            Sign out all sessions
          </Button>
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
        <section className="panel appearance-panel">
          <div>
            <h2>Appearance</h2>
            <p className="muted">A workspace that feels like yours.</p>
          </div>
          <Button variant="outline" onClick={toggle}>
            {dark ? <Sun size={16} /> : <Moon size={16} />}Use{" "}
            {dark ? "light" : "dark"} theme
          </Button>
        </section>
      </div>
    </>
  );
}
