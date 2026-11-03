export interface User {
  id: string;
  name: string;
  email: string;
  createdAt: string;
}
export interface Application {
  id: string;
  userId: string;
  name: string;
  createdAt: string;
}
export interface EventType {
  id: string;
  applicationId: string;
  name: string;
  description: string | null;
  createdAt: string;
}
export interface EndpointConfiguration {
  enabled: boolean;
  rateLimitPerSecond: number;
  concurrencyLimit: number;
  timeoutMs: number;
  maxAttempts: number;
}
export interface IssuedEndpoint {
  endpoint: Endpoint;
  signingSecret: string;
}
export interface Endpoint {
  id: string;
  applicationId: string;
  url: string;
  description: string | null;
  eventTypeIds: string[];
  configuration: EndpointConfiguration;
  signingSecretVersion: number;
  signingSecretRotatedAt: string | null;
  createdAt: string;
}
export interface ApiKey {
  id: string;
  applicationId: string;
  name: string;
  prefix: string;
  createdAt: string;
  revokedAt: string | null;
  replacedById: string | null;
}
export interface IssuedApiKey {
  key: ApiKey;
  secret: string;
}
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface PageParams {
  page?: number;
  size?: number;
}
