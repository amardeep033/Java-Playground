import { client } from "@/lib/api/client";
import type { ApiKey, IssuedApiKey, Page, PageParams } from "@/types/api";
export const keysApi = {
  list: async (appId: string, params: PageParams) =>
    (
      await client.get<Page<ApiKey>>(`/applications/${appId}/api-keys`, {
        params,
      })
    ).data,
  get: async (appId: string, id: string) =>
    (await client.get<ApiKey>(`/applications/${appId}/api-keys/${id}`)).data,
  create: async (appId: string, body: { name: string }) =>
    (await client.post<IssuedApiKey>(`/applications/${appId}/api-keys`, body))
      .data,
  rotate: async (appId: string, id: string) =>
    (
      await client.post<IssuedApiKey>(
        `/applications/${appId}/api-keys/${id}/rotate`,
      )
    ).data,
  update: async (appId: string, id: string, body: { name: string }) =>
    (await client.put<ApiKey>(`/applications/${appId}/api-keys/${id}`, body))
      .data,
  revoke: async (appId: string, id: string) => {
    await client.delete(`/applications/${appId}/api-keys/${id}`);
  },
};
