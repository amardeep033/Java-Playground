import { createContext, useContext, useState, useEffect } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MotionConfig } from "motion/react";
import { ApiError } from "@/lib/api/client";
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (count, error) =>
        count < 1 &&
        (!(error instanceof ApiError) || !error.status || error.status >= 500),
    },
    mutations: { retry: false, gcTime: 0 },
  },
});
const ThemeContext = createContext({ dark: false, toggle: () => {} });
export const useTheme = () => useContext(ThemeContext);
export function AppProviders({ children }: { children: React.ReactNode }) {
  const [dark, setDark] = useState(
    () => localStorage.getItem("event-relay-theme") === "dark",
  );
  useEffect(() => {
    const expired = () => {
      if (window.location.pathname === "/") return;
      queryClient.clear();
      window.location.assign("/");
    };
    window.addEventListener("session-expired", expired);
    return () => window.removeEventListener("session-expired", expired);
  }, []);
  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    localStorage.setItem("event-relay-theme", dark ? "dark" : "light");
  }, [dark]);
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeContext.Provider
        value={{ dark, toggle: () => setDark((value) => !value) }}
      >
        <MotionConfig reducedMotion="user">{children}</MotionConfig>
      </ThemeContext.Provider>
    </QueryClientProvider>
  );
}
