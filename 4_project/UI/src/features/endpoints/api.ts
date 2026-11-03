import { client } from "@/lib/api/client";
import type {
  Endpoint,
  EndpointConfiguration,
  IssuedEndpoint,
  Page,
  PageParams,
} from "@/types/api";
export const endpointsApi = {
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
    body: { url: string; description?: string; eventTypeIds: string[] },
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
    configuration: EndpointConfiguration,
  ) =>
    (
      await client.put<Endpoint>(
        `/applications/${appId}/endpoints/${id}/configuration`,
        configuration,
      )
    ).data,
  rotateSecret: async (appId: string, id: string) =>
    (
      await client.post<IssuedEndpoint>(
        `/applications/${appId}/endpoints/${id}/signing-secret/rotate`,
      )
    ).data,
  remove: async (appId: string, id: string) => {
    await client.delete(`/applications/${appId}/endpoints/${id}`);
  },
};
