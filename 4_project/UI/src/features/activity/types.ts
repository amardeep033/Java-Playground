export type EventStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "NO_SUBSCRIBERS";
export type DeliveryStatus =
  "PENDING" | "RETRY_SCHEDULED" | "SUCCEEDED" | "DEAD";
export type JsonValue =
  string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };
export interface EventSummary {
  id: string;
  eventType: string;
  deliveryCount: number;
  status: EventStatus;
  createdAt: string;
}
export interface AcceptedEvent {
  id: string;
  eventType: string;
  deliveryCount: number;
  createdAt: string;
}
export interface EventDetails {
  id: string;
  applicationId: string;
  eventType: string;
  payload: JsonValue;
  createdAt: string;
  status: EventStatus;
}
export interface Delivery {
  id: string;
  eventId: string;
  endpointId: string;
  eventType: string;
  endpointUrl: string;
  status: DeliveryStatus;
  attemptCount: number;
  httpStatus: number | null;
  error: string | null;
  nextAttemptAt: string | null;
  createdAt: string;
  updatedAt: string;
  replayOfId: string | null;
  replayId: string | null;
  canReplay: boolean;
  replayReason: string | null;
}
export interface Attempt {
  attemptNumber: number;
  httpStatus: number | null;
  error: string | null;
  startedAt: string;
  completedAt: string;
  durationMs: number;
  responseBody: string | null;
  contentType: string | null;
  responseTruncated: boolean;
}
export interface DeliveryDetails {
  delivery: Delivery;
  requestBody: JsonValue;
  attempts: Attempt[];
}
export interface ReplayReceipt {
  id: string;
  eventId: string;
  replayOfId: string;
  createdAt: string;
}
export interface ActivitySummary {
  events: number;
  deliveries: number;
  succeeded: number;
  pending: number;
  failed: number;
  replays: number;
}
export interface ActivityFilters {
  page?: number;
  size?: number;
  status?: string;
  q?: string;
  eventId?: string;
  endpointId?: string;
  includeReplayed?: boolean;
}
