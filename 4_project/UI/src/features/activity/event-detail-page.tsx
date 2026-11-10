import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import {
  CopyButton,
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import { activityApi } from "./api";
import { TrackingNav, TrackingNext } from "./tracking-nav";
import { TrackingRedirect } from "./tracking-redirect";
import { JsonPanel, StatusBadge, Timestamp } from "./shared";
export function EventContent({eventId}:{eventId:string}) {
  const { app } = useWorkspace();
  const query = useQuery({
    queryKey: ["activity", app!.id, "event", eventId],
    queryFn: ({ signal }) => activityApi.event(app!.id, eventId, signal),
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
      <TrackingNav previous={query.data.incomingRequestId?{kind:"incoming",id:query.data.incomingRequestId}:undefined}>
        <TrackingNext view={{kind:"deliveries",id:eventId}}>View in deliveries</TrackingNext>
        {query.data.status!=="SUCCEEDED"&&<TrackingNext view={{kind:"dead-letters",id:eventId}}>View in dead letters</TrackingNext>}
      </TrackingNav>
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

    </>
  );
}
export default function EventDetailPage() {
 const {eventId=""}=useParams();return <TrackingRedirect kind="event" id={eventId} section="events"/>;
}
