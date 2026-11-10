import { type ReactNode, useRef, Children, useEffect } from "react";
import { ChevronDown } from "lucide-react";
import { useTracking, type TrackingView } from "./tracking-context";
export function TrackingNav({previous,children}:{previous?:TrackingView;children?:ReactNode}) {
  const {jump}=useTracking();
  const menu=useRef<HTMLDetailsElement>(null);
  useEffect(()=>{const outside=(event:PointerEvent)=>{if(menu.current&&!menu.current.contains(event.target as Node))menu.current.open=false;};document.addEventListener("pointerdown",outside);return()=>document.removeEventListener("pointerdown",outside);},[]);
  const hasChildren=Children.toArray(children).length>0;
  if(!previous&&!hasChildren)return null;
  const direct=previous?.kind==="event";
  return <div className="inspector-actions">{direct&&<TrackingNext view={previous!}>View in events</TrackingNext>}{(!direct||hasChildren)&&<details className="related-menu" ref={menu} onClick={event=>{if((event.target as HTMLElement).closest("button"))menu.current?.removeAttribute("open");}}>
    <summary>Related records <ChevronDown size={15}/></summary>
    <div className="related-options">
      {previous&&!direct&&<button type="button" onClick={()=>jump(previous)}>{previous.kind==="incoming"?"View in incoming requests":"View in events"}</button>}
      {children}
    </div>
  </details>}</div>;
}
export function TrackingNext({view,children,disabled=false}:{view:TrackingView;children:ReactNode;disabled?:boolean}) {
  const {jump}=useTracking();
  return <button type="button" disabled={disabled} onClick={()=>jump(view)}>{children}</button>;
}
