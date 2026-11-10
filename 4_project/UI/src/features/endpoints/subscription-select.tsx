import {useEffect,useRef,useState} from "react";
import {useInfiniteQuery} from "@tanstack/react-query";
import {ChevronDown} from "lucide-react";
import {eventTypesApi} from "@/features/events/api";
import {useDebounced} from "@/features/activity/shared";
import {Button} from "@/components/ui/button";
import {ErrorMessage,Loading} from "@/components/shared/feedback";
export function SubscriptionSelect({appId,selected,onChange,id}:{id?:string;appId:string;selected:string[];onChange:(ids:string[])=>void}) {
 const ref=useRef<HTMLDetailsElement>(null);const [search,setSearch]=useState("");const q=useDebounced(search);
 const query=useInfiniteQuery({queryKey:["event-types",appId,"subscription-options",q],initialPageParam:0,queryFn:({pageParam})=>eventTypesApi.list(appId,{q,page:pageParam,size:20}),getNextPageParam:last=>last.page+1<last.totalPages?last.page+1:undefined});
 const options=query.data?.pages.flatMap(page=>page.items)||[];
 useEffect(()=>{const close=(e:PointerEvent)=>{if(ref.current&&!ref.current.contains(e.target as Node))ref.current.open=false;};const escape=(e:KeyboardEvent)=>{if(e.key==="Escape"&&ref.current?.open){e.preventDefault();e.stopPropagation();ref.current.open=false;}};document.addEventListener("pointerdown",close);window.addEventListener("keydown",escape,true);return()=>{document.removeEventListener("pointerdown",close);window.removeEventListener("keydown",escape,true);};},[]);
 return <details className="application-multiselect" ref={ref}><summary id={id} role="button" aria-label="Event subscriptions" className="input"><span>{selected.length} event types selected</span><ChevronDown size={16}/></summary><div className="application-multiselect-panel">
 <input className="input" aria-label="Search event type options" placeholder="Search event types…" value={search} onChange={e=>{setSearch(e.target.value);}} onKeyDown={e=>{if(e.key==="Enter")e.preventDefault();}}/>
 {query.isPending?<Loading/>:query.isError?<ErrorMessage error={query.error} retry={()=>void query.refetch()}/>:<><div className="application-multiselect-options" role="group" aria-label="Event type options" onScroll={event=>{const box=event.currentTarget;if(box.scrollHeight-box.scrollTop-box.clientHeight<80&&query.hasNextPage&&!query.isFetchingNextPage)void query.fetchNextPage();}}>{options.map(type=><label className="subscription-option" key={type.id}><input type="checkbox" aria-label={type.name} checked={selected.includes(type.id)} onChange={e=>onChange(e.target.checked?[...selected,type.id]:selected.filter(id=>id!==type.id))}/><span>{type.name}</span></label>)}{!options.length&&<p className="muted">No matching event types.</p>}{query.isFetchingNextPage&&<p role="status" className="muted">Loading more…</p>}</div></>}
 <Button type="button" variant="outline" onClick={()=>{if(ref.current)ref.current.open=false;}}>Done</Button></div></details>;
}
