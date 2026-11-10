import { client } from "@/lib/api/client";
import type {
  Endpoint,
  EndpointConfiguration,
  IssuedEndpoint,
  Page,
  PageParams,
} from "@/types/api";
export const endpointsApi = {
  setActive: async (appId:string,id:string,active:boolean) =>
    (await client.put<Endpoint>(`/applications/${appId}/endpoints/${id}/admin`,{active})).data,
  updateUrl: async (appId:string,id:string,url:string) =>
    (await client.put<Endpoint>(`/applications/${appId}/endpoints/${id}/admin`,{url})).data,
  subscriptions: async (appId:string,id:string,eventTypeIds:string[]) =>
    (await client.put<Endpoint>(`/applications/${appId}/endpoints/${id}/admin`,{eventTypeIds})).data,
  list: async (appId: string, params: PageParams) =>
    (
      await client.get<Page<Endpoint>>(`/applications/${appId}/endpoints`, {
        params,
      })
    ).data,
  get: async (appId: string, id: string) =>
    (await client.get<Endpoint>(`/applications/${appId}/endpoints/${id}`)).data,
  create: async (
    appId: string,
    body: {
      name: string;
      url: string;
      description?: string;
      eventTypeIds: string[];
    },
  ) =>
    (
      await client.post<IssuedEndpoint>(
        `/applications/${appId}/endpoints`,
        body,
      )
    ).data,
  configure: async (
    appId: string,
    id: string,
    configuration: Omit<EndpointConfiguration,"enabled">,
  ) =>
    (
      await client.put<Endpoint>(
        `/applications/${appId}/endpoints/${id}`,
        {configuration},
      )
    ).data,
  rotateSecret: async (appId: string, id: string) =>
    (
      await client.post<IssuedEndpoint>(
        `/applications/${appId}/endpoints/${id}/signing-secret/rotate`,
      )
    ).data,
  update: async (
    appId: string,
    id: string,
    body: { name?: string; description?: string | null; configuration?:Omit<EndpointConfiguration,"enabled"> },
  ) =>
    (await client.put<Endpoint>(`/applications/${appId}/endpoints/${id}`, body))
      .data,
  remove: async (appId: string, id: string) => {
    await client.delete(`/applications/${appId}/endpoints/${id}`);
  },
};
