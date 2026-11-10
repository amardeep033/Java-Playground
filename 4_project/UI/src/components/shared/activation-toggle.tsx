import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ErrorMessage } from "./feedback";

export function ActivationToggle({
  active,
  name,
  save,
  canChange = true,
}: {
  canChange?: boolean;
  active: boolean;
  name: string;
  save: (active: boolean) => Promise<unknown>;
}) {
  const cache = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => save(!active),
    onSuccess: () => {
      void cache.invalidateQueries();
    },
  });
  return (
    <div className="editor-activation">
      <span>{active?"Active":"Inactive"}</span>
      <button type="button" role="switch" aria-checked={active} aria-label={`Activate ${name}`} title={active?"Deactivate":"Activate"} disabled={!canChange||mutation.isPending} onClick={()=>mutation.mutate()} className="activation-switch"><span/></button>
      <ErrorMessage error={mutation.error}/>
    </div>
  );
}
