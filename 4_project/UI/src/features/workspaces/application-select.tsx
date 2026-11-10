import { useEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";
import { Input } from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import type { ApplicationOption } from "./clients-api";

/** A small, complete company catalogue; search only narrows visible options, never selection. */
export function ApplicationSelect({applications,selected,onToggle,disabled=false}:{
  applications:ApplicationOption[];selected:string[];onToggle:(id:string,checked:boolean)=>void;disabled?:boolean;
}) {
  const ref=useRef<HTMLDetailsElement>(null);
  const [search,setSearch]=useState("");
  useEffect(()=>{
    const outside=(event:PointerEvent)=>{if(ref.current&&!ref.current.contains(event.target as Node)) ref.current.open=false;};
    // Handle Escape before Radix Dialog's document capture listener dismisses the editor.
    const escape=(event:KeyboardEvent)=>{if(event.key==="Escape"&&ref.current?.open&&ref.current.contains(event.target as Node)){event.preventDefault();event.stopPropagation();ref.current.open=false;ref.current.querySelector("summary")?.focus();}};
    document.addEventListener("pointerdown",outside);window.addEventListener("keydown",escape,true);
    return()=>{document.removeEventListener("pointerdown",outside);window.removeEventListener("keydown",escape,true);};
  },[]);
  const visible=applications.filter(app=>app.name.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()));
  return <details ref={ref} className="application-multiselect" onToggle={event=>{if(!event.currentTarget.open)setSearch("");}}>
    <summary role="button" aria-label="Application access" className="input"><span>{selected.length?`${selected.length} application${selected.length===1?"":"s"} selected`:"Select applications"}</span><ChevronDown size={16}/></summary>
    <div className="application-multiselect-panel">
      <Input type="search" aria-label="Search application options" placeholder="Search applications…" value={search} onChange={event=>setSearch(event.target.value)} onKeyDown={event=>{if(event.key==="Enter")event.preventDefault();}}/>
      <div className="application-multiselect-options" role="group" aria-label="Company applications">
        {visible.map(app=><label className="subscription-option" key={app.id}><input type="checkbox" aria-label={app.name} checked={selected.includes(app.id)} disabled={disabled} onChange={event=>onToggle(app.id,event.target.checked)}/><span>{app.name}</span>{!app.active&&<small className="badge">Inactive</small>}</label>)}
        {!visible.length&&<p className="muted">{applications.length?"No matching applications.":"No applications created yet."}</p>}
      </div>
      <Button type="button" size="sm" variant="outline" onClick={()=>{if(ref.current)ref.current.open=false;}}>Done</Button>
    </div>
  </details>;
}
