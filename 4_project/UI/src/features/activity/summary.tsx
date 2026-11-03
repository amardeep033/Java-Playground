import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { CheckCircle2, Clock3, Send, AlertCircle } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { activityApi } from "./api";
export function ActivitySummary() {
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
      label: "Events received",
      value: query.data.events,
      icon: Send,
      path: "events",
    },
    {
      label: "Successful deliveries",
      value: query.data.succeeded,
      icon: CheckCircle2,
      path: "deliveries?status=SUCCEEDED",
    },
    {
      label: "Pending deliveries",
      value: query.data.pending,
      icon: Clock3,
      path: "deliveries?status=PENDING",
    },
    {
      label: "Awaiting replay",
      value: query.data.failed,
      icon: AlertCircle,
      path: "dead-letters",
    },
  ];
  return (
    <div className="activity-stats">
      {items.map((item) => (
        <Link
          className="stat-card"
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
  );
}
