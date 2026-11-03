import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { RotateCcw, ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { ErrorMessage } from "@/components/shared/feedback";
import { useWorkspace } from "@/app/layouts/workspace-layout";
import { activityApi } from "./api";
import type { Delivery } from "./types";
export function ReplayButton({ delivery }: { delivery: Delivery }) {
  const { user, app } = useWorkspace();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [requestKey, setRequestKey] = useState("");
  const root = `/users/${user.id}/apps/${app!.id}`;
  const mutation = useMutation({
    mutationFn: () => activityApi.replay(app!.id, delivery.id, requestKey),
    onSuccess: (receipt) => {
      void queryClient.invalidateQueries({ queryKey: ["activity", app!.id] });
      setOpen(false);
      navigate(`${root}/deliveries/${receipt.id}`);
    },
  });
  if (delivery.replayId)
    return (
      <Button variant="outline" size="sm" asChild>
        <Link to={`${root}/deliveries/${delivery.replayId}`}>
          View replay
          <ArrowUpRight size={13} />
        </Link>
      </Button>
    );
  return (
    <>
      <Button
        variant="outline"
        size="sm"
        disabled={!delivery.canReplay}
        title={delivery.replayReason || "Send this failed delivery again"}
        onClick={() => {
          mutation.reset();
          setRequestKey(crypto.randomUUID());
          setOpen(true);
        }}
      >
        <RotateCcw size={13} />
        Replay
      </Button>
      <Dialog
        open={open}
        onOpenChange={(value) => {
          if (!mutation.isPending) setOpen(value);
        }}
      >
        <DialogContent>
          <DialogHeader
            title="Replay failed delivery?"
            description="A new delivery will use the original event body and the endpoint’s current settings and signing secret. The failed delivery and its attempts stay in history."
          />
          <div className="subtle-note break-anywhere">
            {delivery.endpointUrl}
          </div>
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
              disabled={mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              <RotateCcw size={14} />
              {mutation.isPending ? "Queuing replay…" : "Confirm replay"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
