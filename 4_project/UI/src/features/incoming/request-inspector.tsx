import { useQuery } from "@tanstack/react-query";
import { client } from "@/lib/api/client";
import type { Page } from "@/types/api";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { CopyButton, ErrorMessage, Loading } from "@/components/shared/feedback";
import { Timestamp } from "@/features/activity/shared";
import { TrackingNext } from "@/features/activity/tracking-nav";
import type { IncomingRequest } from "./types";
export function RequestInspector({id}:{id:string}) {
 const {app}=useWorkspace();
 const query=useQuery({queryKey:["incoming-request",app!.id,id],queryFn:async({signal})=>{
  const response=await client.get<Page<IncomingRequest>>(`/applications/${app!.id}/incoming-requests`,{params:{q:id,page:0,size:1},signal});
  const entry=response.data.items.find(item=>item.id===id);if(!entry)throw new Error("Incoming request is unavailable.");return entry;
 },refetchInterval:3000});
 if(query.isPending)return <Loading/>;if(query.isError)return <ErrorMessage error={query.error} retry={()=>void query.refetch()}/>;
 const request=query.data;
 return <><div className="inspector-actions"><TrackingNext disabled={request.outcome!=="ACCEPTED"||!request.eventId} view={{kind:"event",id:request.eventId||request.id}}>View in events</TrackingNext></div>
 <section className="panel inspector-section"><h2>Request outcome</h2><p className="muted">{request.reason}</p>
              <dl className="detail-list">
                <dt>Request ID</dt>
                <dd className="id-detail">
                  <code>{request.id}</code>
                  <CopyButton value={request.id} />
                </dd>
                <dt>Result</dt>
                <dd>
                  {request.outcome} · HTTP {request.httpStatus}
                </dd>
                <dt>Reason code</dt>
                <dd>
                  <code>{request.reasonCode}</code>
                </dd>
                <dt>Application</dt>
                <dd>
                  <code>{request.requestedApplicationId}</code>
                </dd>
                <dt>Event type</dt>
                <dd>
                  {request.eventType ||
                    "Unavailable; request did not reach validated ingestion"}
                </dd>
                <dt>API key name</dt>
                <dd>{request.apiKeyName || "—"}</dd>
                <dt>API key ID</dt>
                <dd>
                  <code>{request.apiKeyId || "—"}</code>
                </dd>
                <dt>Credential type</dt>
                <dd>{request.authMethod}</dd>
                <dt>Idempotency key supplied</dt>
                <dd>{request.idempotencyKeyPresent ? "Yes" : "No"}</dd>
                <dt>Received</dt>
                <dd>
                  <Timestamp value={request.receivedAt} />
                </dd>
                <dt>Duration</dt>
                <dd>{request.durationMs} ms</dd>
                <dt>Client IP</dt>
                <dd>{request.clientIp}</dd>
              </dl>
              {request.skippedEndpoints.length > 0 && (
                <div>
                  <p className="muted">
                    Inactive endpoints excluded from this submission. No
                    delivery was created for these destinations.
                  </p>
                  <ul>
                    {request.skippedEndpoints.map((id) => (
                      <li key={id}>
                        <code>{id}</code>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {request.outcome !== "ACCEPTED" &&
                request.outcome !== "PROCESSING" && (
                  <p className="muted">
                    This submission was not accepted. It is not a dead letter
                    and has no replay action.
                  </p>
                )}</section></>;
}
