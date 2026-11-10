import { client } from "@/lib/api/client";
import type { User } from "@/types/api";
import type { RegistrationValues, PasswordValues } from "./schemas";
export const usersApi = {
  register: async (body: RegistrationValues) => {
    return (await client.post<User>("/users", body)).data;
  },
  update: async (id: string, body: { name: string }) =>
    (await client.put<User>(`/users/${id}`, body)).data,
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
  permissions: {
    manageUsers: boolean;
    createApplications: boolean;
  };
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
async function signIn(
  path: string,
  body: { email: string; password: string },
): Promise<Session> {
  return (await client.post<Session>(path, body)).data;
}
export const authApi = {
  me: async () => (await client.get<Session>("/auth/me")).data,
  login: async (body: { email: string; password: string }) =>
    signIn("/auth/login", body),
  logout: async () => {
    await client.post("/auth/logout");
  },
  attempts: async (q = "") =>
    (
      await client.get<LoginAttempt[]>("/auth/login-attempts", {
        params: { q },
      })
    ).data,
};
