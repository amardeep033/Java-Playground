import {
  NavLink,
  Outlet,
  Link,
  useLocation,
  useParams,
  useOutletContext,
  Navigate,
  useNavigate,
} from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  ArrowLeft,
  Activity,
  Inbox,
  Send,
  Box,
  Braces,
  ChevronRight,
  Globe2,
  KeyRound,
  LayoutGrid,
  Menu,
  Moon,
  Settings2,
  Sun,
  X,
} from "lucide-react";
import { authApi, usersApi } from "@/features/auth/api";
import { applicationsApi } from "@/features/applications/api";
import { Button } from "@/components/ui/button";
import { Brand } from "@/components/navigation/brand";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { useTheme } from "@/app/providers/app-providers";
import { initials } from "@/lib/utils";

import type { User, Application } from "@/types/api";

interface WorkspaceContext {
  user: User;
  app?: Application;
}
export const useWorkspace = () => useOutletContext<WorkspaceContext>();
export default function WorkspaceLayout() {
  const session = useQuery({
    queryKey: ["session"],
    queryFn: authApi.me,
    retry: false,
    staleTime: 0,
    refetchInterval: 60_000,
  });
  if (session.isPending) return <Loading />;
  if (session.isError) {
    if (
      session.error instanceof Error &&
      "status" in session.error &&
      session.error.status === 401
    )
      return <Navigate to="/" replace />;
    return (
      <ErrorMessage
        error={session.error}
        retry={() => void session.refetch()}
      />
    );
  }
  return <WorkspaceContent />;
}
function WorkspaceContent() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [logoutError, setLogoutError] = useState<unknown>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  async function logout() {
    setLoggingOut(true);
    try {
      await authApi.logout();
      await queryClient.cancelQueries();
      queryClient.clear();
      navigate("/", { replace: true });
    } catch (error) {
      setLogoutError(error);
    } finally {
      setLoggingOut(false);
    }
  }
  const { userId = "" } = useParams();
  const [menu, setMenu] = useState(false);
  const location = useLocation();
  const { dark, toggle } = useTheme();
  const appId = location.pathname.match(/\/apps\/([^/]+)/)?.[1];
  const user = useQuery({
    queryKey: ["user", userId],
    queryFn: () => usersApi.get(userId),
  });
  const app = useQuery({
    queryKey: ["application", userId, appId],
    queryFn: () => applicationsApi.get(userId, appId!),
    enabled: !!appId,
  });
  const root = `/users/${userId}`;
  const appRoot = `${root}/apps/${appId}`;
  if (user.isPending || (appId && app.isPending)) return <Loading />;
  if (user.isError || (appId && app.isError))
    return (
      <main className="standalone-error">
        <Brand />
        <ErrorMessage
          error={user.error || app.error}
          retry={() => {
            void user.refetch();
            if (appId) void app.refetch();
          }}
        />
        <Button asChild variant="outline">
          <Link to={appId ? `${root}/applications` : "/"}>
            Back to workspace
          </Link>
        </Button>
      </main>
    );
  const nav = (to: string, title: string, Icon: typeof Box) => (
    <NavLink
      to={to}
      onClick={() => setMenu(false)}
      className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
    >
      <Icon size={17} />
      {title}
    </NavLink>
  );
  return (
    <div className="workspace-shell">
      {menu && (
        <button
          className="sidebar-backdrop"
          onClick={() => setMenu(false)}
          aria-label="Close navigation"
        />
      )}
      <aside className={`sidebar ${menu ? "sidebar-open" : ""}`}>
        <div className="sidebar-brand">
          <Link to={`${root}/applications`}>
            <Brand />
          </Link>
          <Button
            className="mobile-only"
            variant="ghost"
            size="icon"
            onClick={() => setMenu(false)}
            aria-label="Close navigation"
          >
            <X size={18} />
          </Button>
        </div>
        <div className="workspace-identity">
          <span className="workspace-avatar">{initials(user.data.name)}</span>
          <div>
            <strong>{user.data.name}’s workspace</strong>
            <span>Personal workspace</span>
          </div>
          <span className="workspace-indicator" />
        </div>
        <nav aria-label="Main navigation">
          <p className="nav-label">Workspace</p>
          {nav(`${root}/applications`, "Applications", LayoutGrid)}
          {appId && (
            <>
              <p className="nav-label">{app.data?.name || "Application"}</p>
              {nav(`${appRoot}/overview`, "Overview", Box)}
              {nav(`${appRoot}/events`, "Events", Send)}
              {nav(`${appRoot}/deliveries`, "Deliveries", Activity)}
              {nav(`${appRoot}/dead-letters`, "Dead letters", Inbox)}
              {nav(`${appRoot}/event-types`, "Event types", Braces)}
              {nav(`${appRoot}/endpoints`, "Endpoints", Globe2)}
              {nav(`${appRoot}/api-keys`, "API keys", KeyRound)}
            </>
          )}
          <p className="nav-label">Account</p>
          {nav(`${root}/settings`, "Profile & security", Settings2)}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-caption">
            <span className="status-dot" />
            Your webhook workspace
          </div>
          <ErrorMessage error={logoutError} />
          <Button
            variant="ghost"
            onClick={() => void logout()}
            disabled={loggingOut}
          >
            <ArrowLeft size={16} />
            {loggingOut ? "Signing out…" : "Sign out"}
          </Button>
        </div>
      </aside>
      <div className="workspace-content">
        <header className="topbar">
          <div className="breadcrumbs">
            <Button
              variant="ghost"
              size="icon"
              className="mobile-only"
              onClick={() => setMenu(true)}
              aria-label="Open navigation"
            >
              <Menu size={19} />
            </Button>
            <Link to={`${root}/applications`}>Workspace</Link>
            <ChevronRight size={13} />
            <span>
              {app.data?.name ||
                (location.pathname.endsWith("/settings")
                  ? "Profile & security"
                  : "Applications")}
            </span>
          </div>
          <div className="topbar-actions">
            <Button
              variant="ghost"
              size="icon"
              onClick={toggle}
              aria-label={
                dark ? "Switch to light theme" : "Switch to dark theme"
              }
            >
              {dark ? <Sun size={17} /> : <Moon size={17} />}
            </Button>
            <Link
              className="account-avatar"
              to={`${root}/settings`}
              aria-label="Profile and security"
            >
              {initials(user.data.name)}
            </Link>
          </div>
        </header>
        <main className="page-content" key={`${userId}-${appId || ""}`}>
          <Outlet
            context={
              { user: user.data, app: app.data } satisfies WorkspaceContext
            }
          />
        </main>
        <footer className="workspace-footer">
          <span>Event Relay</span>
          <span>Make every connection count.</span>
        </footer>
      </div>
    </div>
  );
}
