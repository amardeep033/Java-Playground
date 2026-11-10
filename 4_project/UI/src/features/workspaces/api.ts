import { client } from "@/lib/api/client";
import type { Page } from "@/types/api";
export interface AuditLog {
  id: string;
  actor_id: string;
  actor_name?: string;
  application_id?:string;
  application_name?:string;
  entity_type: string;
  entity_id: string;
  action: string;
  details: Record<string, unknown>;
  created_at: string;
  resource: string;
  display_action: string;
}
export interface AuditOption {value:string;label:string}
export type AuditPageResult=Page<AuditLog>&{actors:AuditOption[];scopes:AuditOption[]};
export const workspacesApi = {
  setMember: async (app: string, userId: string) => {
    await client.put(`/applications/${app}/members`, { userId });
  },
  removeMember: async (app: string, userId: string) => { await client.delete(`/applications/${app}/members/${userId}`); },
  audit: async (clientId:string,filters:{resources:string;actions:string;actors:string;scopes:string;sort:string;q:string;page:number}) =>
    (await client.get<AuditPageResult>(`/clients/${clientId}/audit-logs`, { params: {...filters,size:20} })).data,
};
