import {SubscriptionSelect} from "./subscription-select";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { endpointsApi } from "./api";
import type { Endpoint } from "@/types/api";
import { Field, Input } from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import { ErrorMessage } from "@/components/shared/feedback";
export function RoutingEditor({ endpoint, onSaved }: { endpoint: Endpoint; onSaved: () => void }) {
  const [url, setUrl] = useState(endpoint.url);
  const [ids, setIds] = useState(endpoint.eventTypeIds);
  const destination = useMutation({mutationFn:()=>endpointsApi.updateUrl(endpoint.applicationId,endpoint.id,url),onSuccess:onSaved});
  const subscriptions = useMutation({mutationFn:()=>endpointsApi.subscriptions(endpoint.applicationId,endpoint.id,ids),onSuccess:onSaved});
  return <div className="form-stack"><form className="form-stack" onSubmit={e=>{e.preventDefault();destination.mutate();}}>
    <Field id="endpoint-routing-url" label="Destination URL"><Input id="endpoint-routing-url" type="url" required maxLength={2048} value={url} onChange={(e) => setUrl(e.target.value)} /></Field>
    <ErrorMessage error={destination.error}/><Button disabled={destination.isPending}>Save destination</Button></form>
    <form className="form-stack" onSubmit={e=>{e.preventDefault();subscriptions.mutate();}}>
    <Field id="endpoint-subscriptions" label="Event subscriptions"><SubscriptionSelect id="endpoint-subscriptions" appId={endpoint.applicationId} selected={ids} onChange={setIds}/></Field>
    <p className="field-hint">Changes apply to newly accepted requests. Accepted deliveries keep their original settings.</p>
    <ErrorMessage error={subscriptions.error}/><Button disabled={subscriptions.isPending||ids.length===0}>Save event mappings</Button>
    </form></div>;
}
