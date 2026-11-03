import { client } from "@/lib/api/client";
import type { Page } from "@/types/api";
import type {
  AcceptedEvent,
  ActivityFilters,
  ActivitySummary,
  Delivery,
  DeliveryDetails,
  EventDetails,
  EventSummary,
  JsonValue,
  ReplayReceipt,
} from "./types";
const config = (signal?: AbortSignal) => ({ signal });
export const activityApi = {
  events: async (app: string, params: ActivityFilters, signal?: AbortSignal) =>
    (
      await client.get<Page<EventSummary>>(`/applications/${app}/events`, {
        ...config(signal),
        params,
      })
    ).data,
  event: async (app: string, id: string, signal?: AbortSignal) =>
    (
      await client.get<EventDetails>(`/applications/${app}/events/${id}`, {
        ...config(signal),
        params: { includeDeliveries: false },
      })
    ).data,
  send: async (
    app: string,
    idempotencyKey: string,
    body: { eventType: string; payload: JsonValue },
  ) =>
    (
      await client.post<AcceptedEvent>(`/applications/${app}/events`, body, {
        headers: { "Idempotency-Key": idempotencyKey },
      })
    ).data,
  deliveries: async (
    app: string,
    params: ActivityFilters,
    signal?: AbortSignal,
  ) =>
    (
      await client.get<Page<Delivery>>(`/applications/${app}/deliveries`, {
        ...config(signal),
        params,
      })
    ).data,
  delivery: async (app: string, id: string, signal?: AbortSignal) =>
    (
      await client.get<DeliveryDetails>(
        `/applications/${app}/deliveries/${id}`,
        config(signal),
      )
    ).data,
  summary: async (app: string, signal?: AbortSignal) =>
    (
      await client.get<ActivitySummary>(
        `/applications/${app}/activity-summary`,
        config(signal),
      )
    ).data,
  replay: async (app: string, id: string, idempotencyKey: string) =>
    (
      await client.post<ReplayReceipt>(
        `/applications/${app}/deliveries/${id}/replay`,
        undefined,
        { headers: { "Idempotency-Key": idempotencyKey } },
      )
    ).data,
};
