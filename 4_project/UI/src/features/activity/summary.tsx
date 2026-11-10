import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { CheckCircle2, Clock3, Send, AlertCircle } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { activityApi } from "./api";
export function ActivitySummary({detailed=false}:{detailed?:boolean}) {
  const { app, user } = useWorkspace();
  const query = useQuery({
    queryKey: ["activity", app!.id, "summary"],
    queryFn: ({ signal }) => activityApi.summary(app!.id, signal),
    staleTime: 1000,
    refetchInterval: 3000,
  });
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorMessage error={query.error} retry={() => void query.refetch()} />
    );
  const root = `/users/${user.id}/apps/${app!.id}`;
  const items = [
    {
      tone:"info",
      label: "Events received",
      value: query.data.events,
      icon: Send,
      path: "events",
    },
    {
      tone:"success",
      label: "Successful deliveries",
      value: query.data.succeeded,
      icon: CheckCircle2,
      path: "deliveries?status=SUCCEEDED",
    },
    {
      tone:"warning",
      label: "Pending deliveries",
      value: query.data.pending,
      icon: Clock3,
      path: "deliveries?status=PENDING",
    },
    {
      tone:"danger",
      label: "Awaiting replay",
      value: query.data.failed,
      icon: AlertCircle,
      path: "dead-letters",
    },
  ];
  return (
    <><div className="activity-stats">
      {items.map((item) => (
        <Link
          className={`stat-card status-stat stat-${item.tone}`}
          key={item.label}
          to={`${root}/${item.path}`}
        >
          <div>
            <span>{item.label}</span>
            <item.icon size={17} />
          </div>
          <strong>{item.value}</strong>
        </Link>
      ))}
    </div>
    {detailed&&<section className="panel outcome-panel"><div className="panel-heading"><div><h2>Current delivery outcomes</h2><p className="muted">Latest outcomes across destinations. Replayed failures remain in history.</p></div><span className="badge">Live</span></div>
     <div className="outcome-bar" role="img" aria-label={`${query.data.succeeded} successful, ${query.data.pending} pending, ${query.data.failed} awaiting replay`}>
      {[{tone:"success",count:query.data.succeeded},{tone:"warning",count:query.data.pending},{tone:"danger",count:query.data.failed}].map(item=><span key={item.tone} className={`outcome-${item.tone}`} style={{flexGrow:item.count}}/>)}
     </div><div className="outcome-legend"><span><i className="outcome-success"/>Successful · {query.data.succeeded}</span><span><i className="outcome-warning"/>Pending · {query.data.pending}</span><span><i className="outcome-danger"/>Awaiting replay · {query.data.failed}</span><span>{query.data.replays} replays created</span></div>
    </section>}
    </>
  );
}
