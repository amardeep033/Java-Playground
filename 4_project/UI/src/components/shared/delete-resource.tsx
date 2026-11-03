import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { ErrorMessage } from "./feedback";

export function DeleteResource({
  name,
  description,
  remove,
  onDeleted,
  label = "Delete",
}: {
  name: string;
  description: string;
  remove: () => Promise<void>;
  onDeleted: () => void;
  label?: string;
}) {
  const [open, setOpen] = useState(false);
  const mutation = useMutation({
    mutationFn: remove,
    onSuccess: () => {
      setOpen(false);
      onDeleted();
    },
  });
  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => {
          mutation.reset();
          setOpen(true);
        }}
      >
        <Trash2 size={13} />
        {label}
      </Button>
      <Dialog
        open={open}
        onOpenChange={(value) => {
          if (!mutation.isPending) setOpen(value);
        }}
      >
        <DialogContent>
          <DialogHeader title={`Delete ${name}?`} description={description} />
          <ErrorMessage error={mutation.error} />
          <div className="dialog-actions">
            <Button
              variant="outline"
              disabled={mutation.isPending}
              onClick={() => setOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              Confirm delete
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
