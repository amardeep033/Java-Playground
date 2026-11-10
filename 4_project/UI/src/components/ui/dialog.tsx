import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
export const Dialog = DialogPrimitive.Root;
export const DialogTitle = DialogPrimitive.Title;
export const DialogDescription = DialogPrimitive.Description;
export function DialogContent({ children, className = "", contentRef }: { children: React.ReactNode; className?: string; contentRef?:React.Ref<HTMLDivElement> }) {
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="dialog-overlay" />
      <DialogPrimitive.Content ref={contentRef} className={`dialog-content ${className}`}>
        {children}
        <DialogPrimitive.Close
          className="dialog-close"
          aria-label="Close dialog"
        >
          <X size={18} />
        </DialogPrimitive.Close>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  );
}
export function DialogHeader({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="dialog-header">
      <DialogTitle>{title}</DialogTitle>
      <DialogDescription className={description ? undefined : "sr-only"}>{description || title}</DialogDescription>
    </div>
  );
}
