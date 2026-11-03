import {
  AlertCircle,
  Check,
  Copy,
  Inbox,
  Loader2,
  RefreshCw,
} from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
export function ErrorMessage({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  if (!error) return null;
  const message =
    error instanceof Error
      ? error.message
      : "Something went wrong. Please try again.";
  return (
    <div className="error-message" role="alert">
      <AlertCircle size={17} />
      <div>
        <p>{message}</p>
        {error instanceof ApiError &&
          Object.entries(error.fields).map(([field, text]) => (
            <p key={field}>
              {field}: {text}
            </p>
          ))}
      </div>
      {retry && (
        <Button variant="ghost" size="sm" onClick={retry}>
          <RefreshCw size={14} />
          Retry
        </Button>
      )}
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading" role="status">
      <Loader2 className="animate-spin" size={20} />
      <span>Loading your workspace…</span>
    </div>
  );
}
export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Inbox size={23} />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}
export function CopyButton({
  value,
  label = "Copy ID",
}: {
  value: string;
  label?: string;
}) {
  const [state, setState] = useState<"idle" | "copied" | "failed">("idle");
  async function copy() {
    try {
      await navigator.clipboard.writeText(value);
      setState("copied");
    } catch {
      setState("failed");
    }
  }
  return (
    <Button
      variant="ghost"
      size="sm"
      type="button"
      onClick={() => void copy()}
      aria-label={label}
    >
      {state === "copied" ? <Check size={14} /> : <Copy size={14} />}
      <span>
        {state === "copied"
          ? "Copied"
          : state === "failed"
            ? "Copy failed"
            : label}
      </span>
    </Button>
  );
}
