import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, ArrowUpRight, Clock3, Globe2 } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/shared/page-header";
import {
  CopyButton,
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import { ActivityAccess } from "./access";
import { activityApi } from "./api";
import { JsonPanel, LiveRefresh, StatusBadge, Timestamp } from "./shared";
import { ReplayButton } from "./replay-button";
function DeliveryContent() {
  const { deliveryId = "" } = useParams();
  const { user, app } = useWorkspace();
  const root = `/users/${user.id}/apps/${app!.id}`;
  const query = useQuery({
    queryKey: ["activity", app!.id, "delivery", deliveryId],
    queryFn: ({ signal }) => activityApi.delivery(app!.id, deliveryId, signal),
    staleTime: 1000,
    refetchInterval: 3000,
  });
  if (query.isPending) return <Loading />;
  if (query.isError)
    return (
      <ErrorMessage error={query.error} retry={() => void query.refetch()} />
    );
  const { delivery, attempts, requestBody } = query.data;
  return (
    <>
      <div className="delivery-heading">
        <div className="id-detail">
          <code>{delivery.id}</code>
          <CopyButton value={delivery.id} />
          <StatusBadge status={delivery.status} />
        </div>
        <div className="flex gap-2 items-center">
          <LiveRefresh
            refreshing={query.isFetching}
            refresh={() => void query.refetch()}
          />
          {delivery.status === "DEAD" && <ReplayButton delivery={delivery} />}
        </div>
      </div>
      {delivery.replayOfId && (
        <div className="replay-notice">
          Replay of{" "}
          <Link
            className="text-link"
            to={`${root}/deliveries/${delivery.replayOfId}`}
          >
            {delivery.replayOfId}
            <ArrowUpRight size={12} />
          </Link>
        </div>
      )}
      {delivery.status === "DEAD" &&
        !delivery.canReplay &&
        !delivery.replayId && (
          <p className="field-hint">{delivery.replayReason}</p>
        )}
      <section className="panel delivery-metadata">
        <div className="destination-heading">
          <Globe2 size={18} />
          <strong>{delivery.endpointUrl}</strong>
        </div>
        <dl>
          <div>
            <dt>Event</dt>
            <dd>
              <Link
                className="text-link"
                to={`${root}/events/${delivery.eventId}`}
              >
                <code>{delivery.eventType}</code>
                <ArrowUpRight size={12} />
              </Link>
            </dd>
          </div>
          <div>
            <dt>Attempts</dt>
            <dd>{delivery.attemptCount}</dd>
          </div>
          <div>
            <dt>Latest HTTP status</dt>
            <dd>{delivery.httpStatus || "—"}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>
              <Timestamp value={delivery.createdAt} />
            </dd>
          </div>
        </dl>
        {delivery.error && <p className="delivery-error">{delivery.error}</p>}
        {delivery.nextAttemptAt && (
          <p className="field-hint">
            Next retry: <Timestamp value={delivery.nextAttemptAt} />
          </p>
        )}
      </section>
      <JsonPanel title="Webhook request body" value={requestBody} />
      <section className="panel attempts-panel">
        <div className="panel-heading">
          <h2>
            Delivery attempts{" "}
            <span className="count-badge">{attempts.length}</span>
          </h2>
          <span className="muted">Oldest first</span>
        </div>
        {attempts.length === 0 && (
          <p className="muted">
            {delivery.attemptCount === 0
              ? "Waiting for the delivery worker. Attempts will appear here automatically."
              : "Detailed attempts are unavailable for deliveries processed before this upgrade."}
          </p>
        )}
        {attempts.length > 0 && attempts.length < delivery.attemptCount && (
          <p className="field-hint">
            Some attempt details are still arriving or were processed before
            this upgrade.
          </p>
        )}
        {attempts.map((attempt, index) => (
          <details
            className="attempt-item"
            key={attempt.attemptNumber}
            open={index === attempts.length - 1}
          >
            <summary>
              <span className="attempt-number">{attempt.attemptNumber}</span>
              <strong>Attempt {attempt.attemptNumber}</strong>
              <span
                className={`badge ${attempt.error ? "status-danger" : "status-success"}`}
              >
                {attempt.httpStatus
                  ? `HTTP ${attempt.httpStatus}`
                  : "Network error"}
              </span>
              <span className="attempt-time">
                <Clock3 size={12} />
                {attempt.durationMs} ms
              </span>
              <span className="muted">
                <Timestamp value={attempt.startedAt} />
              </span>
            </summary>
            <div className="attempt-content">
              {attempt.error && (
                <p className="delivery-error">{attempt.error}</p>
              )}
              <div className="response-heading">
                <h3>Response preview</h3>
                {attempt.contentType && <code>{attempt.contentType}</code>}
                {attempt.responseTruncated && (
                  <span className="badge status-warning">
                    Truncated · first 4 KB
                  </span>
                )}
              </div>
              <pre className="code-block">
                {attempt.responseBody === null
                  ? "No text response captured."
                  : attempt.responseBody === ""
                    ? "Empty response body."
                    : attempt.responseBody}
              </pre>
              <p className="field-hint">
                Completed <Timestamp value={attempt.completedAt} />
              </p>
            </div>
          </details>
        ))}
      </section>
    </>
  );
}
export default function DeliveryDetailPage() {
  const { user, app } = useWorkspace();
  return (
    <>
      <Button variant="ghost" size="sm" asChild>
        <Link to={`/users/${user.id}/apps/${app!.id}/deliveries`}>
          <ArrowLeft size={14} />
          All deliveries
        </Link>
      </Button>
      <PageHeader
        eyebrow={app!.name}
        title="Delivery details"
        description="Inspect the request, attempt timeline, and response previews."
      />
      <ActivityAccess>
        <DeliveryContent />
      </ActivityAccess>
    </>
  );
}
