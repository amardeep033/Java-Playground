import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Search, AlertCircle } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { Checkbox } from "@/components/ui/checkbox";
import { ActivityAccess } from "./access";
import { ActivitySummary } from "./summary";
import { activityApi } from "./api";
import { DeliveriesTable } from "./deliveries-table";
import { LiveRefresh, useDebounced } from "./shared";
function DeliveriesContent({ failed }: { failed: boolean }) {
  const { app } = useWorkspace();
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Math.trunc(Number(params.get("page")) || 0));
  const q = params.get("q") || "";
  const status = failed ? "DEAD" : params.get("status") || "";
  const search = useDebounced(q);
  const includeReplayed = !failed || params.get("includeReplayed") === "true";
  const eventId = params.get("eventId") || undefined;
  const endpointId = params.get("endpointId") || undefined;
  function filter(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(name, value);
    if (name !== "page") next.delete("page");
    setParams(next, { replace: true, flushSync: true });
  }
  const query = useQuery({
    queryKey: [
      "activity",
      app!.id,
      "deliveries",
      page,
      status,
      search,
      includeReplayed,
      eventId,
      endpointId,
    ],
    queryFn: ({ signal }) =>
      activityApi.deliveries(
        app!.id,
        {
          page,
          size: 20,
          status,
          q: search,
          includeReplayed,
          eventId,
          endpointId,
        },
        signal,
      ),
    staleTime: 1000,
    refetchInterval: 3000,
  });
  return (
    <>
      <ActivitySummary />
      {failed && (
        <div className="subtle-note">
          <AlertCircle size={17} />
          <span>
            Inspect permanent failures and exhausted retries. Replaying creates
            a linked delivery and preserves this history.
          </span>
        </div>
      )}
      <div className="activity-toolbar">
        <label className="search-field">
          <Search size={15} />
          <input
            aria-label="Search deliveries"
            maxLength={200}
            value={q}
            onChange={(event) => filter("q", event.target.value)}
            placeholder="Search ID, type, or destination…"
          />
        </label>
        {failed ? (
          <label className="filter-checkbox">
            <Checkbox
              checked={includeReplayed}
              onCheckedChange={(checked) =>
                filter("includeReplayed", String(checked === true))
              }
            />
            Include replayed
          </label>
        ) : (
          <select
            className="input filter-select"
            aria-label="Delivery status"
            value={status}
            onChange={(event) => filter("status", event.target.value)}
          >
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="RETRY_SCHEDULED">Retry scheduled</option>
            <option value="SUCCEEDED">Succeeded</option>
            <option value="DEAD">Failed</option>
          </select>
        )}
        <LiveRefresh
          refreshing={query.isFetching}
          refresh={() => void query.refetch()}
        />
      </div>
      {query.isPending ? (
        <Loading />
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DeliveriesTable
          data={query.data}
          page={page}
          onPage={(value) => filter("page", String(value))}
          failed={failed}
        />
      )}
    </>
  );
}
export default function DeliveriesPage({
  failed = false,
}: {
  failed?: boolean;
}) {
  const { app } = useWorkspace();
  return (
    <>
      <PageHeader
        eyebrow={app!.name}
        title={failed ? "Dead letters" : "Deliveries"}
        description={
          failed
            ? "Understand what failed. Choose what to send again."
            : "Follow every destination, attempt, and outcome in real time."
        }
      />
      <ActivityAccess>
        <DeliveriesContent failed={failed} />
      </ActivityAccess>
    </>
  );
}
