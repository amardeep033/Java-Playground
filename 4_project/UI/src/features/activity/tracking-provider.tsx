import { useEffect, useRef, type ReactNode } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { RequestInspector } from "@/features/incoming/request-inspector";
import { TrackingContext, type TrackingKind, type TrackingView } from "./tracking-context";
import { EventContent } from "./event-detail-page";
import { DeliveryContent } from "./delivery-detail-page";
const titles: Partial<Record<TrackingKind,string>> = {incoming:"Incoming request",event:"Event details",delivery:"Delivery details","dead-letter":"Dead letter details"};
export function TrackingProvider({children}:{children:ReactNode}) {
  const {app,user}=useWorkspace();
  const [params,setParams]=useSearchParams();
  const navigate=useNavigate();
  const contentRef=useRef<HTMLDivElement>(null);
  const kind=params.get("inspect") as TrackingKind|null;
  const id=params.get("inspectId");
  const view=kind&&titles[kind]&&id&&/^[0-9a-f-]{36}$/i.test(id)?{kind,id}:undefined;
  useEffect(()=>{if(kind&&id){contentRef.current?.scrollTo(0,0);}},[kind,id]);
  function close() {
    const next=new URLSearchParams(params);next.delete("inspect");next.delete("inspectId");setParams(next,{replace:true});
  }
  function open(next:TrackingView) {
    const search=new URLSearchParams(params);search.set("inspect",next.kind);search.set("inspectId",next.id);setParams(search,{replace:true});
  }
  function jump(next:TrackingView) {
    const section=next.kind==="incoming"?"incoming-requests":next.kind==="event"?"events":next.kind.startsWith("dead")?"dead-letters":"deliveries";
    const search=new URLSearchParams();
    if(next.kind==="deliveries"||next.kind==="dead-letters") search.set("eventId",next.id);
    else {search.set("q",next.id);search.set("selected",next.id);}
    if(next.kind.startsWith("dead"))search.set("includeReplayed","true");
    navigate(`/users/${user.id}/apps/${app!.id}/${section}?${search}`);
  }
  return <TrackingContext.Provider value={{open,close,jump}}>
    {children}
    <Dialog open={!!view&&!!app} onOpenChange={value=>{if(!value)close();}}>
      <DialogContent className="tracking-dialog" contentRef={contentRef}>
        <DialogHeader title={view?titles[view.kind]!:"Details"} description=""/>
        {view&&<div className="tracking-content" key={`${app?.id}-${view.kind}-${view.id}`}>
          {view.kind==="incoming"?<RequestInspector id={view.id}/>:view.kind==="event"?<EventContent eventId={view.id}/>:<DeliveryContent deliveryId={view.id} deadLetter={view.kind==="dead-letter"}/>}
        </div>}
      </DialogContent>
    </Dialog>
  </TrackingContext.Provider>;
}
