import { client } from "@/lib/api/client";
import type { EventType, Page, PageParams } from "@/types/api";
export const eventTypesApi = {
  list: async (appId: string, params: PageParams) =>
    (
      await client.get<Page<EventType>>(`/applications/${appId}/event-types`, {
        params,
      })
    ).data,
  get: async (appId: string, id: string) =>
    (await client.get<EventType>(`/applications/${appId}/event-types/${id}`))
      .data,
  create: async (appId: string, body: { name: string; description?: string }) =>
    (await client.post<EventType>(`/applications/${appId}/event-types`, body))
      .data,
  remove: async (appId: string, id: string) => {
    await client.delete(`/applications/${appId}/event-types/${id}`);
  },
};
