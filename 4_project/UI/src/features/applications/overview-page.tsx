import { Link } from "react-router-dom";
import { useQueries } from "@tanstack/react-query";
import { Braces, Globe2, KeyRound } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { CopyButton, ErrorMessage } from "@/components/shared/feedback";
import { eventTypesApi } from "@/features/events/api";
import { endpointsApi } from "@/features/endpoints/api";
import { keysApi } from "@/features/api-keys/api";
import { ActivityAccess } from "@/features/activity/access";
import { ActivitySummary } from "@/features/activity/summary";
import { date } from "@/lib/utils";
export default function OverviewPage() {
  const { user, app } = useWorkspace();
  const id = app!.id;
  const queries = useQueries({
    queries: [
      {
        queryKey: ["event-types", id, "summary"],
        queryFn: () => eventTypesApi.list(id, { page: 0, size: 1 }),
      },
      {
        queryKey: ["endpoints", id, "summary"],
        queryFn: () => endpointsApi.list(id, { page: 0, size: 1 }),
      },
      {
        queryKey: ["api-keys", id, "summary"],
        queryFn: () => keysApi.list(id, { page: 0, size: 1 }),
      },
    ],
  });
  const modules = [
    {
      title: "Event types",
      description: "Define what happens in your application.",
      path: "event-types",
      icon: Braces,
    },
    {
      title: "Endpoints",
      description: "Choose where your events will go.",
      path: "endpoints",
      icon: Globe2,
    },
    {
      title: "API keys",
      description: "Create and manage application credentials.",
      path: "api-keys",
      icon: KeyRound,
    },
  ];
  return (
    <>
      <PageHeader
        eyebrow="Application overview"
        title={app!.name}
        description="Your integration, at a glance."
      />
      <ActivityAccess>
        <ActivitySummary detailed />
      </ActivityAccess>
      <div className="section-heading activity-section">
        <h2>Application configuration</h2>
      </div>
      <div className="stats-grid">
        {modules.map((module, i) => (
          <Link
            key={module.path}
            className={`stat-card configuration-stat configuration-${module.path}`}
            to={`/users/${user.id}/apps/${id}/${module.path}`}
          >
            <div>
              <span>{module.title}</span>
              <module.icon size={19} />
            </div>
            <strong>
              {queries[i].isPending
                ? "…"
                : queries[i].isError
                  ? "—"
                  : queries[i].data?.totalElements}
            </strong>
            <span className="stat-link">
              Manage {module.title.toLowerCase()}
            </span>
          </Link>
        ))}
      </div>
      {queries.some((query) => query.isError) && (
        <ErrorMessage
          error={queries.find((query) => query.isError)?.error}
          retry={() => queries.forEach((query) => void query.refetch())}
        />
      )}
      <div className="overview-grid">
        <section className="panel">
          <div className="panel-heading"><h2>Event flow</h2><span className="badge">Monitoring</span></div>
          <p className="muted">Trace every accepted request through its destinations and final outcomes.</p>
          {[{title:"Incoming requests",text:"Accepted and rejected submissions",path:"incoming-requests"},{title:"Events",text:"Validated payloads and destinations",path:"events"},{title:"Deliveries",text:"Final outcomes and attempt history",path:"deliveries"},{title:"Dead letters",text:"Inspect failures and replay",path:"dead-letters"}].map((stage,index)=><Link className="setup-row" key={stage.path} to={`/users/${user.id}/apps/${id}/${stage.path}`}><span className="step-number">{index+1}</span><div><strong>{stage.title}</strong><p>{stage.text}</p></div></Link>)}
        </section>
        <section className="panel details-panel">
          <div className="panel-heading">
            <h2>Application details</h2>
          </div>
          <dl>
            <dt>Name</dt>
            <dd>{app!.name}</dd>
            <dt>Application ID</dt>
            <dd className="id-detail">
              <code>{id}</code>
              <CopyButton value={id} />
            </dd>
            <dt>Created</dt>
            <dd>{date(app!.createdAt)}</dd>
            <dt>Owner</dt>
            <dd>
              {user.name}
              <span className="muted block">{user.email}</span>
            </dd>
          </dl>
        </section>
      </div>
    </>
  );
}
