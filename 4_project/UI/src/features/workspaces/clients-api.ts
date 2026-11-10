import { client } from "@/lib/api/client";
import type { Client, Page, User } from "@/types/api";
export interface ClientUser { id: string; name: string; email: string; role: "USER" | "ADMIN"; created_at: string; applicationIds:string[] }
export interface ApplicationOption {id:string;name:string;active:boolean}
export type ClientUserList=Page<ClientUser>&{applications:ApplicationOption[]};
export interface NewClientUser { name:string;email:string;password:string;role:"USER"|"ADMIN";applicationIds:string[] }
export const clientsApi = {
  get: async (id:string)=>(await client.get<Client>(`/clients/${id}`)).data,
  users: async (id:string,q:string,page:number,role="")=>(await client.get<ClientUserList>(`/clients/${id}/users`,{params:{q,page,size:20,role}})).data,
  createUser: async (id:string,body:NewClientUser)=>(await client.post<User>(`/clients/${id}/users`,body)).data,
  updateUser: async (id:string,user:string,body:{name?:string;email:string;role:"USER"|"ADMIN"})=>{await client.put(`/clients/${id}/users/${user}`,body);},
  resetPassword: async (id:string,user:string,newPassword:string)=>{await client.put(`/clients/${id}/users/${user}/password`,{newPassword});},
  removeUser: async (id:string,user:string)=>{await client.delete(`/clients/${id}/users/${user}`);},
};
