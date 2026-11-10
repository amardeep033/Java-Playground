import {statusColumn} from "@/components/shared/column-filter";
import { useTracking } from "./tracking-context";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/shared/data-table";
import type { Page } from "@/types/api";
import type { Delivery } from "./types";
import { StatusBadge, Timestamp } from "./shared";
import { ReplayButton } from "./replay-button";
export function DeliveriesTable({
  data,
  page,
  onPage,
  failed = false,
  status="",onStatus,
}: {
  data: Page<Delivery>;
  page: number;
  onPage: (page: number) => void;
  failed?: boolean;
  status?:string;onStatus?:(value:string)=>void;
}) {
  const {open}=useTracking();
  const columns: ColumnDef<Delivery>[] = [
    {
      header: "Delivery",
      accessorKey: "id",
      cell: ({ row }) => (
        <div>
          <code>{row.original.id.slice(0, 8)}…</code>
          {row.original.replayOfId && (
            <span className="replay-label">Replay</span>
          )}
        </div>
      ),
    },
    {
      header: "Destination",
      accessorKey: "endpointUrl",
      cell: ({ row }) => (
        <div className="delivery-destination">
          <span title={row.original.endpointUrl}>
            {row.original.endpointUrl}
          </span>
          <code>{row.original.eventType}</code>
        </div>
      ),
    },
    {
      header: "Status",
      accessorKey: "status",
      cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    { header: "Attempts", accessorKey: "attemptCount" },
    {
      header: "HTTP",
      accessorKey: "httpStatus",
      cell: ({ row }) => <code>{row.original.httpStatus || "—"}</code>,
    },
    {
      header: "Updated",
      accessorKey: "updatedAt",
      cell: ({ getValue }) => <Timestamp value={getValue<string>()} />,
    },
    {
      header: "Actions",
      id: "actions",
      cell: ({ row }) => (
        <div className="flex gap-2">
          {row.original.status === "DEAD" && (
            <ReplayButton delivery={row.original} />
          )}
        </div>
      ),
    },
  ];
  return (
    <DataTable
      data={data}
      columns={columns}
      columnFilters={onStatus&&!failed?{status:statusColumn(status,onStatus,[{value:"PENDING",label:"Pending"},{value:"SUCCEEDED",label:"Succeeded"},{value:"DEAD",label:"Failed"}])}:undefined}
      onInspect={delivery=>open({kind:failed?"dead-letter":"delivery",id:delivery.id})}
      inspectLabel={failed?"View dead letter":"View delivery"}
      page={page}
      onPage={onPage}
      emptyTitle={failed ? "No failed deliveries" : "No matching deliveries"}
      emptyDescription={
        failed
          ? "Permanently failed deliveries appear here for inspection and replay."
          : "Send an event or adjust the current filters."
      }
    />
  );
}
