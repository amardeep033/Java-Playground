import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Box, Check, ChevronDown, Settings2 } from "lucide-react";
import { applicationsApi } from "@/features/applications/api";
import { useDebounced } from "@/features/activity/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/field";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { initials } from "@/lib/utils";
import type { Application, User } from "@/types/api";
function useDisclosure() {
  const ref=useRef<HTMLDetailsElement>(null);
  useEffect(()=>{
    const outside=(event:PointerEvent)=>{if(ref.current&&!ref.current.contains(event.target as Node)) ref.current.open=false;};
    const escape=(event:KeyboardEvent)=>{if(event.key==="Escape"&&ref.current?.open){ref.current.open=false;ref.current.querySelector("summary")?.focus();}};
    document.addEventListener("pointerdown",outside);document.addEventListener("keydown",escape);
    return()=>{document.removeEventListener("pointerdown",outside);document.removeEventListener("keydown",escape);};
  },[]);
  return ref;
}
export function AppSwitcher({ userId, app, onSelect }: {userId:string;app?:Application;onSelect:(app:Application)=>void}) {
  const ref=useDisclosure();
  const [search,setSearch]=useState("");const [page,setPage]=useState(0);const q=useDebounced(search);
  const [open,setOpen]=useState(false);
  const query=useQuery({queryKey:["application-switcher",userId,q,page],queryFn:()=>applicationsApi.list(userId,{q,page,size:10}),enabled:open});
  return <details ref={ref} className="app-switcher" onToggle={event=>setOpen(event.currentTarget.open)}>
    <summary role="button" aria-label="Select application" title="Switch application"><Box size={16}/><span>{app?.name || "Select application"}</span><ChevronDown size={14}/></summary>
    <div className="app-switcher-panel">
      <Input aria-label="Find application" value={search} placeholder="Find an application…" onChange={event=>{setSearch(event.target.value);setPage(0);}} />
      <div className="app-options" aria-label="Available applications">
        {query.isPending?<Loading/>:query.isError?<ErrorMessage error={query.error}/>:query.data.items.length===0?<p className="muted">No applications found.</p>:query.data.items.map(item=><button key={item.id} className="app-option" aria-label={`Select ${item.name}`} aria-pressed={item.id===app?.id} onClick={()=>{onSelect(item);if(ref.current) ref.current.open=false;}}><Box size={15}/><span><strong>{item.name}</strong><small>{item.active?"Active":"Inactive"}</small></span>{item.id===app?.id&&<Check size={15}/>}</button>)}
      </div>
      {query.data && query.data.totalPages>1 && <div className="switcher-pagination"><Button size="sm" variant="ghost" disabled={page===0} onClick={()=>setPage(page-1)}>Previous apps</Button><span>{page+1}/{query.data.totalPages}</span><Button size="sm" variant="ghost" disabled={page+1>=query.data.totalPages} onClick={()=>setPage(page+1)}>Next apps</Button></div>}
    </div>
  </details>;
}
export function ProfileMenu({user,role,onOpen}:{user:User;role:string;onOpen:()=>void}) {
  return <div className="sidebar-bottom"><button className="profile-launcher" aria-label="Profile & security" title={`${user.name} · ${role}`} onClick={onOpen}><span className="workspace-avatar">{initials(user.name)}</span><span className="profile-menu-identity"><strong>{user.name}</strong><small>{role}</small></span><Settings2 size={17}/></button></div>;
}
