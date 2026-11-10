import {ColumnFilter} from "@/components/shared/column-filter";
import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { ArrowDown, ArrowUp } from "lucide-react";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { PageHeader } from "@/components/shared/page-header";
import { ListFilters } from "@/components/shared/list-filters";
import { DataTable } from "@/components/shared/data-table";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { Timestamp, useDebounced } from "@/features/activity/shared";
import { workspacesApi, type AuditLog, type AuditOption } from "./api";
const changeLabels:Record<string,string>={ENDPOINT_UPDATED:"Endpoint updated",CONFIG_UPDATED:"Configuration updated",EVENT_TYPE_MAPPINGS_UPDATED:"Event type mappings updated"};
const resources=[{value:"user",label:"User"},{value:"app",label:"App"},{value:"apikey",label:"API key"},{value:"endpoint",label:"Endpoint"},{value:"eventtype",label:"Event type"}];
export default function AuditPage() {
  const {user}=useWorkspace();
  const [search,setSearch]=useState("");const [page,setPage]=useState(0);
  const [expanded,setExpanded]=useState("");
  const [filters,setFilters]=useState({resources:[] as string[],actions:[] as string[],actors:[] as string[],scopes:[] as string[]});
  const [sort,setSort]=useState("desc");const q=useDebounced(search);
  const params={resources:filters.resources.join(","),actions:filters.actions.join(","),actors:filters.actors.join(","),scopes:filters.scopes.join(","),sort,q,page};
  const query=useQuery({queryKey:["audit-logs",user.clientId,params],queryFn:()=>workspacesApi.audit(user.clientId,params),placeholderData:keepPreviousData});
  function header(key:keyof typeof filters,label:string,options:AuditOption[]) {
    return <ColumnFilter expanded={expanded===key} onExpanded={open=>setExpanded(current=>open?key:current===key?"":current)} label={label} options={options} selected={filters[key]} onChange={values=>{setFilters(current=>({...current,[key]:values}));setPage(0);}}/>;
  }
  function renderHeader(id:string) {
    if(id==="created_at")return <button className="column-sort" aria-label={`Sort time ${sort==="desc"?"ascending":"descending"}`} onClick={()=>{setSort(sort==="desc"?"asc":"desc");setPage(0);}}>Time {sort==="desc"?<ArrowDown size={14}/>:<ArrowUp size={14}/>}</button>;
    if(id==="resource")return header("resources","Resource",resources);
    if(id==="display_action")return header("actions","Action",["create","update","delete"].map(value=>({value,label:value[0].toUpperCase()+value.slice(1)})));
    if(id==="actor_name")return header("actors","Actor",query.data?.actors||[]);
    if(id==="scope")return header("scopes","Scope",[{value:"overall",label:"Overall"},...(query.data?.scopes||[])]);
    return "Change";
  }
  const columns:ColumnDef<AuditLog>[]=[
    {header:"Time",accessorKey:"created_at",cell:({getValue})=><span className="audit-time"><Timestamp value={getValue<string>()}/></span>},
    {header:"Resource",accessorKey:"resource",cell:({getValue})=>resources.find(item=>item.value===getValue())?.label||"User"},
    {header:"Action",accessorKey:"display_action",cell:({getValue})=><span className="badge">{getValue<string>()}</span>},
    {header:"Actor",accessorKey:"actor_name",cell:({getValue})=>getValue<string>()||"Deleted user"},
    {header:"Scope",id:"scope",cell:({row})=>row.original.application_name||"Overall"},
    {header:"Change",accessorKey:"details",cell:({row})=><div>{changeLabels[row.original.action]&&<strong className="audit-change-label">{changeLabels[row.original.action]}</strong>}<code className="audit-change">{JSON.stringify(row.original.details)}</code></div>},
  ];
  return <>
    <PageHeader title="Audit logs" description="Company changes. Filter individual columns to narrow the results."/>
    <ListFilters label="audit logs" search={search} onSearch={value=>{setSearch(value);setPage(0);}}/>
    {query.isPending?<Loading/>:query.isError?<ErrorMessage error={query.error} retry={()=>void query.refetch()}/>:<DataTable data={query.data} columns={columns} page={page} onPage={setPage} emptyTitle="No changes recorded" emptyDescription="Adjust column filters or make an audited change." renderHeader={renderHeader} keepHeaders/>}
  </>;
}
