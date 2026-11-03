import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import WelcomePage from "./welcome-page";
import { authApi, usersApi } from "./api";
import { ApiError } from "@/lib/api/client";
function renderWelcome() {
  vi.spyOn(authApi, "me").mockRejectedValue(
    new ApiError("Please sign in", 401),
  );
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { mutations: { retry: false } } })
      }
    >
      <MemoryRouter>
        <WelcomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
describe("workspace access", () => {
  it("validates registration before making a request", async () => {
    const register = vi.spyOn(usersApi, "register");
    renderWelcome();
    await userEvent.click(
      screen.getByRole("button", { name: "Create your workspace" }),
    );
    expect(await screen.findByText("Enter your name.")).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });
  it("shows wrong-password feedback without hiding the login form", async () => {
    vi.spyOn(authApi, "login").mockRejectedValue(
      new ApiError("Email or password is incorrect.", 401),
    );
    renderWelcome();
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    await userEvent.type(
      screen.getByLabelText("Email address"),
      "alice@example.com",
    );
    await userEvent.type(
      screen.getByLabelText("Password"),
      "incorrect-password",
    );
    await userEvent.click(
      screen.getAllByRole("button", { name: "Sign in" })[1],
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Email or password is incorrect.",
    );
    expect(screen.getByLabelText("Email address")).toBeVisible();
  });
});
