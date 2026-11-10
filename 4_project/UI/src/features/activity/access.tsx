// Access is enforced by the authenticated layout and API; no repeated session banner.
export function ActivityAccess({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
