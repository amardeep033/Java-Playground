import { client } from "@/lib/api/client";
import type { User } from "@/types/api";
import type { RegistrationValues, PasswordValues } from "./schemas";
export const usersApi = {
  register: async (body: RegistrationValues) =>
    (await client.post<User>("/users", body)).data,
  get: async (id: string) => (await client.get<User>(`/users/${id}`)).data,
  changePassword: async (id: string, body: PasswordValues) => {
    await client.put(`/users/${id}/password`, {
      currentPassword: body.currentPassword,
      newPassword: body.newPassword,
    });
  },
  remove: async (id: string) => {
    await client.delete(`/users/${id}`);
  },
};

export interface Session {
  user: User;
  role: "USER" | "ADMIN";
  expiresAt: string;
}
export interface LoginAttempt {
  attempted_at: string;
  success: boolean;
  reason: string;
  ip_address: string;
}
export const authApi = {
  me: async () => (await client.get<Session>("/auth/me")).data,
  login: async (body: { email: string; password: string }) =>
    (await client.post<Session>("/auth/login", body)).data,
  logout: async (all = false) => {
    await client.post(all ? "/auth/logout-all" : "/auth/logout");
  },
  attempts: async () =>
    (await client.get<LoginAttempt[]>("/auth/login-attempts")).data,
};
