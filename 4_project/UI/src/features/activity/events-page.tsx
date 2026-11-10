import {statusColumn} from "@/components/shared/column-filter";
import { useTracking } from "./tracking-context";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Search } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { ActivityAccess } from "./access";
import { activityApi } from "./api";
import { ActivitySummary } from "./summary";
import { SendEventButton } from "./send-event";
import { LiveRefresh, StatusBadge, Timestamp, useDebounced } from "./shared";
import type { EventSummary } from "./types";
function EventsContent() {
  const { app } = useWorkspace();
  const {open}=useTracking();
  const [params, setParams] = useSearchParams();
  const q = params.get("q") || "";
  const status = params.get("status") || "";
  const page = Math.max(0, Math.trunc(Number(params.get("page")) || 0));
  const search = useDebounced(q);
  function filter(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(name, value);
    if (name !== "page") next.delete("page");
    setParams(next, { replace: true, flushSync: true });
  }
  const query = useQuery({
    queryKey: ["activity", app!.id, "events", page, status, search],
    queryFn: ({ signal }) =>
      activityApi.events(
        app!.id,
        { page, size: 20, status, q: search },
        signal,
      ),
    placeholderData:previous=>previous,
    staleTime: 1000,
    refetchInterval: 3000,
  });
  const columns: ColumnDef<EventSummary>[] = [
    {
      header: "Event",
      accessorKey: "id",
      cell: ({ row }) => (
        <code>{row.original.id.slice(0, 8)}…</code>
      ),
    },
    {
      header: "Type",
      accessorKey: "eventType",
      cell: ({ getValue }) => <code>{getValue<string>()}</code>,
    },
    {
      header: "Status",
      accessorKey: "status",
      cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    { header: "Destinations", accessorKey: "deliveryCount" },
    {
      header: "Received",
      accessorKey: "createdAt",
      cell: ({ getValue }) => <Timestamp value={getValue<string>()} />,
    },
  ];
  return (
    <>
      <ActivitySummary />
      <div className="activity-toolbar">
        <label className="search-field">
          <Search size={15} />
          <input
            aria-label="Search events"
            value={q}
            maxLength={200}
            onChange={(event) => filter("q", event.target.value)}
            placeholder="Search by event ID or type…"
          />
        </label>
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
        <DataTable
          data={query.data}
          columns={columns}
          columnFilters={{status:statusColumn(status,value=>filter("status",value),["PENDING","SUCCEEDED","FAILED","NO_SUBSCRIBERS"].map(value=>({value,label:value})))}}
          onInspect={event=>open({kind:"event",id:event.id})}
          inspectLabel="View event"
          page={page}
          onPage={(value) => filter("page", String(value))}
          emptyTitle={
            q || status ? "No matching events" : "Your first event starts here"
          }
          emptyDescription={
            q || status
              ? "Try another search or status filter."
              : "Send an event to your subscribed endpoints, then follow its deliveries here."
          }
        />
      )}
    </>
  );
}
export default function EventsPage() {
  const { app } = useWorkspace();
  return (
    <>
      <PageHeader
        eyebrow={app!.name}
        title="Events"
        action={<SendEventButton />}
      />
      <ActivityAccess>
        <EventsContent />
      </ActivityAccess>
    </>
  );
}
