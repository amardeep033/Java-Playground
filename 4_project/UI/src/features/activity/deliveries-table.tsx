import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import { ArrowUpRight } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
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
}: {
  data: Page<Delivery>;
  page: number;
  onPage: (page: number) => void;
  failed?: boolean;
}) {
  const { user, app } = useWorkspace();
  const root = `/users/${user.id}/apps/${app!.id}`;
  const columns: ColumnDef<Delivery>[] = [
    {
      header: "Delivery",
      accessorKey: "id",
      cell: ({ row }) => (
        <div>
          <Link
            className="resource-link"
            to={`${root}/deliveries/${row.original.id}`}
          >
            <code>{row.original.id.slice(0, 8)}…</code>
            <ArrowUpRight size={12} />
          </Link>
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
          <Link
            className="button button-ghost button-sm"
            to={`${root}/deliveries/${row.original.id}`}
          >
            Inspect
          </Link>
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
