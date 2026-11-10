import { TrackingProvider } from "@/features/activity/tracking-provider";
import {
  NavLink,
  Outlet,
  Link,
  useLocation,
  useParams,
  Navigate,
  useNavigate,
} from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, useContext, useState, useEffect, type ReactNode } from "react";
import SettingsPage from "@/features/settings/settings-page";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import {
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
  Sun,
  X,
  ShieldCheck,
} from "lucide-react";
import { authApi, usersApi, type Session } from "@/features/auth/api";
import { AppSwitcher, ProfileMenu } from "@/components/navigation/workspace-controls";
import { clientsApi } from "@/features/workspaces/clients-api";
import { applicationsApi } from "@/features/applications/api";
import { Button } from "@/components/ui/button";
import { Brand } from "@/components/navigation/brand";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { useTheme } from "@/app/providers/app-providers";

import type { User, Application } from "@/types/api";

interface WorkspaceContext {
  user: User;
  app?: Application;
  canAdmin: boolean;
  canManageUsers: boolean;
  canCreateApplications: boolean;
  appLoading?: boolean;
}
const Workspace = createContext<WorkspaceContext | null>(null);
export function useWorkspace() { const value=useContext(Workspace); if(!value) throw new Error("Workspace required"); return value; }
function NavCategory({ title, children }: { title:string; children:ReactNode }) {
  const [open,setOpen]=useState(true);
  return <section className="nav-category"><button className="nav-category-toggle" aria-expanded={open} onClick={()=>setOpen(!open)}>{title}<ChevronRight size={14} className={open?"expanded":""}/></button>{open&&<div>{children}</div>}</section>;
}
export function WorkspaceHome() {
  const {app,appLoading,canAdmin}=useWorkspace();
  if(appLoading) return <Loading/>;
  if(app) return <Navigate to={`../apps/${app.id}/overview`} replace/>;
  return <section className="panel"><h1>{canAdmin?"Create your first application":"No applications assigned"}</h1><p className="muted">{canAdmin?"Add an application to configure webhooks and monitor deliveries.":"Your company administrator can assign application access."}</p>{canAdmin&&<Button asChild><Link to="../applications">Applications</Link></Button>}</section>;
}
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
  return <WorkspaceContent viewer={session.data} />;
}
function WorkspaceContent({ viewer }: { viewer: Session }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [logoutError, setLogoutError] = useState<unknown>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const [profileOpen,setProfileOpen]=useState(false);
  async function logout() {
    setLoggingOut(true);
    try {
      await authApi.logout();
      await queryClient.cancelQueries();
      queryClient.clear();
      navigate("/", {
        replace: true,
      });
    } catch (error) {
      setLogoutError(error);
    } finally {
      setLoggingOut(false);
    }
  }
  const { userId = "", clientId } = useParams();
  const canManageUsers=viewer.permissions?.manageUsers === true;
  const canCreateApplications=viewer.permissions?.createApplications === true;
  const [menu, setMenu] = useState(false);
  const location = useLocation();
  const { dark, toggle } = useTheme();
  const routeAppId=location.pathname.match(/\/apps\/([^/]+)/)?.[1];
  const selectionKey=`event-relay.selected-app.${viewer.user.id}.${userId}`;
  const [selection,setSelection]=useState<{key:string;id?:string}>(()=>({key:selectionKey,id:routeAppId || sessionStorage.getItem(selectionKey)||undefined}));
  const initialApps=useQuery({queryKey:["application-context-default",userId],queryFn:()=>applicationsApi.list(userId,{page:0,size:1}),enabled:!routeAppId && !(selection.key===selectionKey&&selection.id)});
  const appId=routeAppId || (selection.key===selectionKey?selection.id:undefined) || initialApps.data?.items[0]?.id;
  useEffect(()=>{if(appId){sessionStorage.setItem(selectionKey,appId);setSelection(current=>current.key===selectionKey&&current.id===appId?current:{key:selectionKey,id:appId});}},[appId,selectionKey]);
  const user = useQuery({
    queryKey: ["user", userId],
    queryFn: () => usersApi.get(userId),
  });
  const app = useQuery({
    queryKey: ["application", userId, appId],
    queryFn: () => applicationsApi.get(userId, appId!),
    enabled: !!appId,
    refetchInterval: 60_000,
  });
  const client = useQuery({
    queryKey: ["client", clientId],
    queryFn: () => clientsApi.get(clientId!),
    enabled: !!clientId,
    staleTime: 0,
    refetchInterval: 60_000,
  });
  const canAdmin=viewer.user.role === "ADMIN";
  const root = `/users/${userId}`;
  const appRoot = `${root}/apps/${appId}`;
  const home=`${root}/overview`;
  useEffect(()=>{if(location.pathname.endsWith("/settings")){setProfileOpen(true);navigate(home,{replace:true});}},[location.pathname,home,navigate]);
  useEffect(()=>{
    if(!routeAppId && app.isError && selection.id){sessionStorage.removeItem(selectionKey);setSelection({key:selectionKey});}
  },[routeAppId,app.isError,selection.id,selectionKey]);
  if (user.isPending || (routeAppId && app.isPending)) return <Loading />;
  if (user.isError || (routeAppId && app.isError))
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
  // Do not mount management forms for a role that cannot use them, including direct URLs.
  if(location.pathname === `${root}/clients` && !canManageUsers) return <Navigate to={`${root}/applications`} replace />;
  if(clientId && !canAdmin) return <Navigate to={home} replace />;
  if(clientId) {
    if(client.isPending) return <Loading />;
    if(client.isError) return <ErrorMessage error={client.error} retry={() => void client.refetch()} />;
    if(client.data.role !== "ADMIN") return <Navigate to={`${root}/applications`} replace />;
  }
  if(routeAppId && location.pathname.endsWith("/members") && !canAdmin) return <Navigate to={`${appRoot}/overview`} replace />;
  if(!canAdmin && (location.pathname.endsWith("/applications") || location.pathname.endsWith("/audit-logs"))) return <Navigate to={home} replace />;
  if(location.pathname.endsWith("/members")) return <Navigate to={canAdmin?`${root}/clients/${viewer.user.clientId}`:home} replace />;
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
    <Workspace.Provider value={{user:user.data,app:app.data,canAdmin,canManageUsers,canCreateApplications,appLoading:initialApps.isFetching||!!appId&&app.isPending}}>
    <TrackingProvider key={`${userId}-${appId||""}`}><div className="workspace-shell">
      {menu && (
        <button
          className="sidebar-backdrop"
          onClick={() => setMenu(false)}
          aria-label="Close navigation"
        />
      )}
      <aside className={`sidebar ${menu ? "sidebar-open" : ""}`}>
        <div className="sidebar-brand">
          <Link to={home}>
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
        <nav aria-label="Main navigation">
          {nav(app.data?`${appRoot}/overview`:home, "Overview", Box)}
          {app.data && <>
            <NavCategory title="Monitoring">
              {nav(`${appRoot}/incoming-requests`, "Incoming requests", Inbox)}
              {nav(`${appRoot}/events`, "Events", Send)}
              {nav(`${appRoot}/deliveries`, "Deliveries", Activity)}
              {nav(`${appRoot}/dead-letters`, "Dead letters", Inbox)}
            </NavCategory>
            <NavCategory title="Configuration">
              {nav(`${appRoot}/api-keys`, "API keys", KeyRound)}
              {nav(`${appRoot}/event-types`, "Event types", Braces)}
              {nav(`${appRoot}/endpoints`, "Endpoints", Globe2)}
            </NavCategory>
          </>}
          {canAdmin && <NavCategory title="Workspace">
            {nav(`${root}/clients/${viewer.user.clientId}`, "Users & access", ShieldCheck)}
            {nav(`${root}/applications`, "Applications", LayoutGrid)}
            {nav(`${root}/audit-logs`, "Audit logs", Activity)}
          </NavCategory>}
        </nav>
        <ProfileMenu user={viewer.user} role={viewer.user.role} onOpen={()=>{setProfileOpen(true);setMenu(false);}}/>
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
            <Link to={home}>Workspace</Link>
            <ChevronRight size={13} />
            <span>
              {routeAppId ? app.data?.name || "Application" : location.pathname.endsWith("/settings") ? "Profile & security" : clientId ? client.data?.name || "Client" : location.pathname.endsWith("/clients") ? "Clients" : location.pathname.endsWith("/audit-logs") ? "Audit logs" : location.pathname.endsWith("/overview") ? "Overview" : "Applications"}
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
            <AppSwitcher userId={userId} app={app.data} onSelect={chosen=>{
              sessionStorage.setItem(selectionKey,chosen.id);setSelection({key:selectionKey,id:chosen.id});
              if(routeAppId){const section=location.pathname.split(`/apps/${routeAppId}/`)[1];navigate(`${root}/apps/${chosen.id}/${section && !section.includes("/")?section:"overview"}`);}
            }}/>

          </div>
        </header>
        <main className="page-content" key={`${userId}-${routeAppId || ""}`}>
          {routeAppId && app.data && !app.data.active && (
            <div className="admin-workspace-notice" role="status">
              Application inactive. New events and replays are paused; accepted
              deliveries continue.{canAdmin ? " Reactivate it from Applications." : " Contact your client administrator to reactivate it."}
            </div>
          )}
          <Outlet
            context={
              { user: user.data, app: app.data, canAdmin, canManageUsers, canCreateApplications } satisfies WorkspaceContext
            }
          />
        </main>
        <footer className="workspace-footer">
          <span>Event Relay</span>
          <span>Make every connection count.</span>
        </footer>
      </div>
    </div>
    <Dialog open={profileOpen} onOpenChange={setProfileOpen}><DialogContent className="profile-overlay">
      <DialogHeader title="Profile & security" description="Manage your account and review recent sign-ins."/>
      <Workspace.Provider value={{user:viewer.user,app:app.data,canAdmin,canManageUsers,canCreateApplications}}><SettingsPage overlay/></Workspace.Provider>
      <div className="profile-signout"><ErrorMessage error={logoutError}/><Button variant="outline" disabled={loggingOut} onClick={()=>void logout()}>{loggingOut?"Signing out…":"Sign out"}</Button></div>
    </DialogContent></Dialog>
    </TrackingProvider></Workspace.Provider>
  );
}
