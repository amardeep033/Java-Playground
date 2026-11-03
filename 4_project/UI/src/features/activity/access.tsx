import { ShieldCheck } from "lucide-react";
// Dashboard activity uses the HttpOnly session; external clients continue to use API keys.
export function ActivityAccess({ children }: { children: React.ReactNode }) {
  return (
    <>
      <div className="activity-access-row">
        <span>
          <ShieldCheck size={15} />
          Signed in with your workspace session
        </span>
      </div>
      {children}
    </>
  );
}
