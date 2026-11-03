import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import {
  CopyButton,
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import { ActivityAccess } from "./access";
import { activityApi } from "./api";
import { DeliveriesTable } from "./deliveries-table";
import { JsonPanel, LiveRefresh, StatusBadge, Timestamp } from "./shared";
function EventContent() {
  const { eventId = "" } = useParams();
  const { app } = useWorkspace();
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ["activity", app!.id, "event", eventId],
    queryFn: ({ signal }) => activityApi.event(app!.id, eventId, signal),
    staleTime: 1000,
    refetchInterval: 3000,
  });
  const deliveries = useQuery({
    queryKey: ["activity", app!.id, "event-deliveries", eventId, page],
    queryFn: ({ signal }) =>
      activityApi.deliveries(app!.id, { eventId, page, size: 20 }, signal),
    staleTime: 1000,
    refetchInterval: 3000,
  });
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorMessage error={query.error} retry={() => void query.refetch()} />
    );
  return (
    <>
      <section className="panel event-identity">
        <div>
          <span className="eyebrow">{query.data.eventType}</span>
          <div className="id-detail">
            <code>{query.data.id}</code>
            <CopyButton value={query.data.id} />
          </div>
        </div>
        <div>
          <StatusBadge status={query.data.status} />
          <p className="field-hint">
            Received <Timestamp value={query.data.createdAt} />
          </p>
        </div>
      </section>
      <JsonPanel title="Event payload" value={query.data.payload} />
      <div className="section-heading activity-section">
        <h2>Delivery history</h2>
        <LiveRefresh
          refreshing={deliveries.isFetching}
          refresh={() => {
            void query.refetch();
            void deliveries.refetch();
          }}
        />
      </div>
      {deliveries.isPending ? (
        <Loading />
      ) : deliveries.isError ? (
        <ErrorMessage
          error={deliveries.error}
          retry={() => void deliveries.refetch()}
        />
      ) : (
        <DeliveriesTable data={deliveries.data} page={page} onPage={setPage} />
      )}
    </>
  );
}
export default function EventDetailPage() {
  const { user, app } = useWorkspace();
  return (
    <>
      <Button variant="ghost" size="sm" asChild>
        <Link to={`/users/${user.id}/apps/${app!.id}/events`}>
          <ArrowLeft size={14} />
          All events
        </Link>
      </Button>
      <PageHeader
        eyebrow={app!.name}
        title="Event details"
        description="The original payload and the complete delivery history."
      />
      <ActivityAccess>
        <EventContent />
      </ActivityAccess>
    </>
  );
}
