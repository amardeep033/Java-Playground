import { client } from "@/lib/api/client";
import type { Application, Page, PageParams } from "@/types/api";
export const applicationsApi = {
  list: async (userId: string, params: PageParams) =>
    (
      await client.get<Page<Application>>(`/users/${userId}/applications`, {
        params,
      })
    ).data,
  get: async (userId: string, appId: string) =>
    (await client.get<Application>(`/users/${userId}/applications/${appId}`))
      .data,
  create: async (userId: string, body: { name: string }) =>
    (await client.post<Application>(`/users/${userId}/applications`, body))
      .data,
  remove: async (userId: string, appId: string) => {
    await client.delete(`/users/${userId}/applications/${appId}`);
  },
};
