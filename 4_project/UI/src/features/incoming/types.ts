export interface IncomingRequest {
  id: string;
  applicationId: string | null;
  requestedApplicationId: string;
  receivedAt: string;
  durationMs: number;
  httpStatus: number;
  outcome: "PROCESSING" | "ACCEPTED" | "REJECTED" | "ERROR";
  reasonCode: string;
  reason: string;
  eventId: string | null;
  eventType: string | null;
  authMethod: string;
  apiKeyId: string | null;
  apiKeyName: string | null;
  duplicateOfId: string | null;
  idempotencyKeyPresent: boolean;
  clientIp: string;
  skippedEndpoints: string[];
}
