import {statusColumn} from "@/components/shared/column-filter";
import { Pencil } from "lucide-react";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { clientsApi, type ClientUser, type NewClientUser } from "./clients-api";
import { ApplicationSelect } from "./application-select";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { workspacesApi } from "./api";
import { PageHeader } from "@/components/shared/page-header";
import { DeleteResource } from "@/components/shared/delete-resource";
import { ListFilters } from "@/components/shared/list-filters";
import { DataTable } from "@/components/shared/data-table";
import { ErrorMessage, Loading } from "@/components/shared/feedback";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { Field, Input } from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import { useDebounced } from "@/features/activity/shared";
const blank:NewClientUser={name:"",email:"",password:"",role:"USER",applicationIds:[]};
export default function ClientUsersPage() {
  const {clientId=""}=useParams();const {user}=useWorkspace();const cache=useQueryClient();const navigate=useNavigate();
  const [roleFilter,setRoleFilter]=useState("");
  const [search,setSearch]=useState("");const [page,setPage]=useState(0);const q=useDebounced(search);
  const [action,setAction]=useState<"create"|"account"|null>(null);
  const [target,setTarget]=useState<ClientUser|null>(null);const [password,setPassword]=useState("");
  const [form,setForm]=useState<NewClientUser>(blank);
  const company=useQuery({queryKey:["client",clientId],queryFn:()=>clientsApi.get(clientId)});
  const rows=useQuery({placeholderData:previous=>previous,queryKey:["client-users",clientId,q,page,roleFilter],queryFn:()=>clientsApi.users(clientId,q,page,roleFilter)});
  const refresh=()=>{void cache.invalidateQueries();};
  const save=useMutation({mutationFn:async()=>{
    if(action==="create") await clientsApi.createUser(clientId,form);
    else if(action==="account") await clientsApi.updateUser(clientId,target!.id,{name:form.name,email:form.email,role:form.role});
  },onSuccess:()=>{if(action!=="account"){setAction(null);setForm(blank);}else if(target){setTarget({...target,name:form.name,email:form.email,role:form.role,applicationIds:form.role===target.role?target.applicationIds:[]});}refresh();}});
  const resetPassword=useMutation({mutationFn:()=>clientsApi.resetPassword(clientId,target!.id,password),onSuccess:()=>{setPassword("");if(target?.id===user.id){cache.clear();navigate("/");}else refresh();}});
  const mapping=useMutation({
    mutationFn:({id,mapped}:{id:string;mapped:boolean})=>mapped?workspacesApi.setMember(id,target!.id):workspacesApi.removeMember(id,target!.id),
    onMutate:change=>{
      const previous=target?{id:target.id,applicationIds:target.applicationIds}:null;
      setTarget(current=>current?{...current,applicationIds:change.mapped?[...new Set([...current.applicationIds,change.id])]:current.applicationIds.filter(id=>id!==change.id)}:null);
      return previous;
    },
    onError:(_error,_change,previous)=>{if(previous)setTarget(current=>current?.id===previous.id?{...current,applicationIds:previous.applicationIds}:current);},
    onSettled:refresh,
  });
  const open=(kind:NonNullable<typeof action>,account?:ClientUser)=>{setTarget(account||null);setPassword("");setForm(account?{...blank,name:account.name,email:account.email,role:account.role}:blank);save.reset();resetPassword.reset();mapping.reset();setAction(kind);};
  const columns:ColumnDef<ClientUser>[]=[{header:"Name",accessorKey:"name",cell:({row})=><button className="resource-link" onClick={()=>open("account",row.original)}>{row.original.name}</button>},{header:"Email",accessorKey:"email"},{header:"Role",accessorKey:"role"},{header:"User ID",accessorKey:"id"},{header:"Manage",id:"actions",cell:({row})=><Button variant="ghost" size="icon" aria-label={`Edit ${row.original.name}`} onClick={()=>open("account",row.original)}><Pencil size={16}/></Button>}];
  return <>
    <PageHeader title="Users & access" description={company.data?.name || "Manage your company’s accounts and application access."} action={<Button disabled={!rows.data} onClick={()=>open("create")}>Create user</Button>}/>
    <ListFilters label="client users" search={search} onSearch={value=>{setSearch(value);setPage(0);}}/>
    {rows.isPending?<Loading/>:rows.isError?<ErrorMessage error={rows.error}/>:<DataTable columnFilters={{role:{...statusColumn(roleFilter,value=>{setRoleFilter(value);setPage(0);},[{value:"ADMIN",label:"Admin"},{value:"USER",label:"User"}]),label:"Role"}}} data={rows.data} columns={columns} page={page} onPage={setPage} emptyTitle="No matching users" emptyDescription="Create an account inside your company."/>}
    <Dialog open={!!action} onOpenChange={value=>{if(!value&&!save.isPending&&!resetPassword.isPending&&!mapping.isPending)setAction(null);}}><DialogContent className="user-editor">
      <DialogHeader title={action==="create"?"Create user":"Edit user"} description={action==="create"?"Admins access all company apps. Users access only mapped apps.":target?.email || ""}/>
      <form className="form-stack" onSubmit={event=>{event.preventDefault();save.mutate();}}>
        {<Field id="new-name" label="Full name"><Input id="new-name" required maxLength={120} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></Field>}
        {<Field id="user-email" label="Email address"><Input id="user-email" type="email" required maxLength={320} value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></Field>}
        {<Field id="user-role" label="Role"><select id="user-role" className="input" value={form.role} onChange={e=>setForm({...form,role:e.target.value as NewClientUser["role"],applicationIds:[]})}><option value="USER">USER — mapped apps</option><option value="ADMIN">ADMIN — all company apps</option></select></Field>}
        {action!=="account"&&<Field id="new-password" label="Initial password" hint="Use 12–128 characters."><Input id="new-password" type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></Field>}
        {action==="create"&&form.role==="USER"&&<section><h3>Application access</h3><ApplicationSelect applications={rows.data?.applications||[]} selected={form.applicationIds} onToggle={(id,checked)=>setForm(current=>({...current,applicationIds:checked?[...current.applicationIds,id]:current.applicationIds.filter(value=>value!==id)}))}/></section>}
        <ErrorMessage error={save.error}/><div className="dialog-actions"><Button disabled={save.isPending||mapping.isPending}>{action==="create"?"Create user":"Save changes"}</Button></div>
      </form>
      {action==="account"&&target&&<>
        {target.role==="USER"&&<section className="editor-section"><h3>Application access</h3><p className="muted">Changes are saved immediately. Save a role change before editing mappings.</p><ErrorMessage error={mapping.error}/><ApplicationSelect applications={rows.data?.applications||[]} selected={target.applicationIds} disabled={mapping.isPending||save.isPending||form.role!==target.role} onToggle={(id,mapped)=>mapping.mutate({id,mapped})}/>{mapping.isPending&&<p role="status" className="muted">Saving access…</p>}</section>}
        <section className="editor-section"><h3>Reset password</h3><form className="form-stack" onSubmit={event=>{event.preventDefault();resetPassword.mutate();}}><Field id="reset-password" label="New password" hint="Changing a password revokes login renewal on all devices."><Input id="reset-password" type="password" autoComplete="new-password" required minLength={12} maxLength={128} value={password} onChange={e=>setPassword(e.target.value)}/></Field><ErrorMessage error={resetPassword.error}/>{resetPassword.isSuccess&&<p role="status">Password updated.</p>}<div><Button disabled={resetPassword.isPending}>Reset password</Button></div></form></section>
        <section className="danger-zone"><h3>Delete user</h3><p>Removes account access while retaining history. Keep at least one company admin.</p><DeleteResource name="user" label="Delete user" description="Removes account access while retaining history. Keep at least one company admin. This cannot be undone in the UI." remove={()=>clientsApi.removeUser(clientId,target.id)} onDeleted={()=>{if(target.id===user.id){cache.clear();navigate("/");}else{setAction(null);refresh();}}}/></section>
      </>}
    </DialogContent></Dialog>
  </>;
}
