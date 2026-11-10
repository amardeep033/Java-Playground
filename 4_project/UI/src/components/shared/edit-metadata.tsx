import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { Field, Input, Textarea } from "@/components/ui/field";
import { ErrorMessage } from "./feedback";
export interface Metadata {
  name?: string;
  description?: string | null;
}
export interface MetadataProps {
  title: string;
  name?: string;
  description?: string | null;
  editName?: boolean;
  editDescription?: boolean;
  note?: string;
  save: (body: Metadata) => Promise<unknown>;
  onSaved: () => void;
}
const schema = z.object({
  name: z.string().trim().max(120).optional(),
  description: z.string().max(500).optional(),
});
export function MetadataForm({ props, close }: { props: MetadataProps; close: () => void }) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: props.name || "",
      description: props.description || "",
    },
  });
  const mutation = useMutation({
    mutationFn: props.save,
    onSuccess: () => {
      props.onSaved();
      close();
    },
  });
  return (
    <form
      className="form-stack"
      onSubmit={handleSubmit((values) => {
        if (props.editName && !values.name) {
          setError("name", { message: "Enter a name." });
          return;
        }
        mutation.mutate({
          ...(props.editName ? { name: values.name } : {}),
          ...(props.editDescription
            ? { description: values.description || null }
            : {}),
        });
      })}
    >
      {props.note && <p className="field-hint">{props.note}</p>}
      {props.editName && (
        <Field id="metadata-name" label="Name" error={errors.name?.message}>
          <Input id="metadata-name" autoFocus {...register("name")} />
        </Field>
      )}
      {props.editDescription && (
        <Field
          id="metadata-description"
          label="Description"
          error={errors.description?.message}
        >
          <Textarea id="metadata-description" {...register("description")} />
        </Field>
      )}
      <ErrorMessage error={mutation.error} />
      <div className="dialog-actions">
        <Button disabled={mutation.isPending}>Save changes</Button>
      </div>
    </form>
  );
}
export function MetadataEditor({
  open,
  onOpenChange,
  ...props
}: MetadataProps & { open: boolean; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader
          title={props.title}
          description="Update the editable details for this resource."
        />
        {open && (
          <MetadataForm props={props} close={() => onOpenChange(false)} />
        )}
      </DialogContent>
    </Dialog>
  );
}
export function EditMetadata(props: MetadataProps) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="ghost" size="icon" aria-label="Edit" title={props.title} onClick={() => setOpen(true)}>
        <Pencil size={13} />
      </Button>
      <MetadataEditor {...props} open={open} onOpenChange={setOpen} />
    </>
  );
}
