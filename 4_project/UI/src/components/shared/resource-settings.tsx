import { useState, type ReactNode } from "react";
import { Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { MetadataForm, type MetadataProps } from "./edit-metadata";

/** One entry point for resource changes; callers supply only permitted controls. */
export function ResourceSettings({ name, metadata, children, open: controlledOpen, onOpenChange, showTrigger = true }: {
  name: string;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  showTrigger?: boolean;
  metadata?: MetadataProps;
  children?: ReactNode | ((close: () => void) => ReactNode);
}) {
  const [localOpen,setLocalOpen]=useState(false);
  const open=controlledOpen ?? localOpen;
  const setOpen=onOpenChange ?? setLocalOpen;
  const close=()=>setOpen(false);
  return <>
    {showTrigger && <Button variant="ghost" size="icon" aria-label="Edit" title={`Edit ${name}`} onClick={()=>setOpen(true)}><Pencil size={16} /></Button>}
    <Dialog open={open} onOpenChange={setOpen}><DialogContent className="resource-editor">
      <DialogHeader title={metadata?.title || `Manage ${name}`} description="" />
      {open && <>
        {metadata && <section className="editor-card"><h3>Details</h3><MetadataForm props={metadata} close={close} /></section>}
        {children && <div className="resource-management">{typeof children === "function" ? children(close) : children}</div>}
      </>}
    </DialogContent></Dialog>
  </>;
}
