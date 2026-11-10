import {statusColumn} from "@/components/shared/column-filter";
import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { client } from "@/lib/api/client";
import type { Page } from "@/types/api";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable } from "@/components/shared/data-table";
import { ListFilters } from "@/components/shared/list-filters";
import {
  ErrorMessage,
  Loading,
} from "@/components/shared/feedback";
import type { IncomingRequest } from "./types";
import { useTracking } from "@/features/activity/tracking-context";
import {
  LiveRefresh,
  Timestamp,
  useDebounced,
} from "@/features/activity/shared";

function RequestHistory({
  applicationId,
}: {
  applicationId?: string;
}) {
  const [page, setPage] = useState(0);
  const [params] = useSearchParams();
  const [search, setSearch] = useState(() => params.get("q") || "");
  const q = useDebounced(search);
  const [status, setStatus] = useState("");
  const [keyName, setKeyName] = useState("");
  const apiKeyName = useDebounced(keyName);
  const [authMethod, setAuthMethod] = useState("");
  const [httpStatus, setHttpStatus] = useState("");
  const {open}=useTracking();
  const query = useQuery<Page<IncomingRequest>>({
    placeholderData:previous=>previous,
    queryKey: [
      "incoming-requests",
      applicationId,
      page,
      status,
      q,
      apiKeyName,
      authMethod,
      httpStatus,
    ],
    queryFn: async ({ signal }) =>
      (
        await client.get<Page<IncomingRequest>>(
          `/applications/${applicationId}/incoming-requests`,
          {
            params: {
              page,
              size: 20,
              status,
              q,
              apiKeyName,
              authMethod,
              httpStatus: httpStatus || undefined,
            },
            signal,
          },
        )
      ).data,
    refetchInterval: 3000,
    staleTime: 1000,
  });
  const columns: ColumnDef<IncomingRequest>[] = [
    {
      header: "Request",
      accessorKey: "id",
      cell: ({ row }) => (
        <code>{row.original.id.slice(0, 8)}…</code>
      ),
    },
    {
      header: "Result",
      accessorKey: "outcome",
      cell: ({ row }) => (
        <span className="badge">
          {row.original.outcome}
        </span>
      ),
    },
    {
      header: "API key",
      accessorKey: "apiKeyName",
      cell: ({ row }) => (
        <span>
          {row.original.apiKeyName ||
            (row.original.authMethod === "API_KEY"
              ? "Unknown key"
              : row.original.authMethod === "DASHBOARD"
                ? "Dashboard"
                : "No credential")}
        </span>
      ),
    },
    {header:"Credential",accessorKey:"authMethod"},
    {header:"HTTP",accessorKey:"httpStatus"},
    {
      header: "Event type",
      accessorKey: "eventType",
      cell: ({ getValue }) => <code>{getValue<string>() || "—"}</code>,
    },
    {
      header: "Reason",
      accessorKey: "reason",
      cell: ({ row }) => (
        <span>
          {row.original.reason}
          {row.original.skippedEndpoints.length > 0 && (
            <small className="muted">
              {" "}
              · {row.original.skippedEndpoints.length} inactive endpoint(s)
            </small>
          )}
        </span>
      ),
    },
    ...(!applicationId
      ? [
          {
            header: "Application",
            accessorKey: "requestedApplicationId",
            cell: ({ row }: { row: { original: IncomingRequest } }) => (
              <code>{row.original.requestedApplicationId}</code>
            ),
          },
        ]
      : []),
    {
      header: "Received",
      accessorKey: "receivedAt",
      cell: ({ getValue }) => <Timestamp value={getValue<string>()} />,
    },
  ];
  return (
    <>
      <PageHeader
        title="Incoming requests"
        description={
          applicationId
            ? "Every event submission and its acceptance or rejection."
            : "Event submissions across all applications, including unknown application IDs."
        }
      />
      <p className="muted">
        Rejected requests cannot be replayed. Correct the request and submit it
        again.
      </p>
      <ListFilters hideStatus
        label="incoming requests"
        search={search}
        onSearch={(value) => {
          setSearch(value);
          setPage(0);
        }}
        status={status}
        onStatus={(value) => {
          setStatus(value);
          setPage(0);
        }}
        options={[
          { value: "PROCESSING", label: "Processing" },
          { value: "ACCEPTED", label: "Accepted" },
          { value: "REJECTED", label: "Rejected" },
          { value: "ERROR", label: "Server error" },
        ]}
      />
      <LiveRefresh
        refreshing={query.isFetching}
        refresh={() => void query.refetch()}
      />
      {query.isPending ? (
        <Loading />
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DataTable
          data={query.data}
          columns={columns}
          columnFilters={{
            outcome:{...statusColumn(status,value=>{setStatus(value);setPage(0);},["PROCESSING","ACCEPTED","REJECTED","ERROR"].map(value=>({value,label:value}))),label:"Result"},
            apiKeyName:{label:"API key",text:keyName,onText:value=>{setKeyName(value);setPage(0);}},
            authMethod:{...statusColumn(authMethod,value=>{setAuthMethod(value);setPage(0);},["API_KEY","DASHBOARD","NONE"].map(value=>({value,label:value}))),label:"Credential"},
            httpStatus:{label:"HTTP",single:true,options:[102,202,400,401,403,404,409,413,415,500,503].map(value=>({value:String(value),label:String(value)})),selected:httpStatus?[httpStatus]:[],onChange:values=>{setHttpStatus(values[0]||"");setPage(0);}}
          }}
          onInspect={request=>open({kind:"incoming",id:request.id})}
          inspectLabel="View incoming request"
          page={page}
          onPage={setPage}
          emptyTitle={
            q || status ? "No matching requests" : "No incoming requests yet"
          }
          emptyDescription="New event submissions will appear here. Earlier requests are not backfilled."
        />
      )}

    </>
  );
}
export default function IncomingRequestsPage() {
  const { app } = useWorkspace();
  return (
    <RequestHistory
      applicationId={app!.id}
    />
  );
}
