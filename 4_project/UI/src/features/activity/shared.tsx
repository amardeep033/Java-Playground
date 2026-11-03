import { useEffect, useState } from "react";
import { format, parseISO } from "date-fns";
import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { DeliveryStatus, EventStatus } from "./types";

const labels: Record<DeliveryStatus | EventStatus, string> = {
  PENDING: "Pending",
  SUCCEEDED: "Succeeded",
  FAILED: "Failed",
  DEAD: "Failed",
  RETRY_SCHEDULED: "Retry scheduled",
  NO_SUBSCRIBERS: "No subscribers",
};
export function StatusBadge({
  status,
}: {
  status: DeliveryStatus | EventStatus;
}) {
  const variant =
    status === "SUCCEEDED"
      ? "success"
      : status === "DEAD" || status === "FAILED"
        ? "danger"
        : status === "RETRY_SCHEDULED"
          ? "warning"
          : status === "PENDING"
            ? "info"
            : "neutral";
  return (
    <span className={`badge status-${variant}`}>
      <span className="badge-dot" />
      {labels[status]}
    </span>
  );
}
export function Timestamp({ value }: { value: string }) {
  return (
    <time dateTime={value} title={value}>
      {format(parseISO(value), "MMM d, HH:mm:ss")}
    </time>
  );
}
export function LiveRefresh({
  refreshing,
  refresh,
}: {
  refreshing: boolean;
  refresh: () => void;
}) {
  return (
    <div className="live-refresh">
      <span>
        <span className="status-dot" />
        Live · every 3s
      </span>
      <Button variant="ghost" size="sm" onClick={refresh} disabled={refreshing}>
        <RefreshCw size={14} className={refreshing ? "animate-spin" : ""} />
        Refresh
      </Button>
    </div>
  );
}
export function useDebounced(value: string) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), 250);
    return () => window.clearTimeout(timer);
  }, [value]);
  return debounced;
}
export function JsonPanel({ title, value }: { title: string; value: unknown }) {
  return (
    <section className="panel payload-panel">
      <div className="panel-heading">
        <h2>{title}</h2>
        <span className="badge">JSON</span>
      </div>
      <pre className="code-block">{JSON.stringify(value, null, 2)}</pre>
    </section>
  );
}
