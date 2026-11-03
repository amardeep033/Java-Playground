import { Workflow } from "lucide-react";
export function Brand({ inverse = false }: { inverse?: boolean }) {
  return (
    <span className={`brand ${inverse ? "brand-inverse" : ""}`}>
      <span className="brand-symbol">
        <Workflow size={21} strokeWidth={1.8} />
      </span>
      Event Relay
      <span className="brand-dot" />
    </span>
  );
}
