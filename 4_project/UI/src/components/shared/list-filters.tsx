import {ColumnFilter,statusColumn} from "./column-filter";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
export function ListFilters({
  label,
  search,
  onSearch,
  status,
  onStatus,
  options = [],
  hideStatus=false,
}: {
  label: string;
  search: string;
  onSearch: (value: string) => void;
  status?: string;
  onStatus?: (value: string) => void;
  options?: { value: string; label: string }[];
  hideStatus?:boolean;
}) {
  return (
    <div className="activity-toolbar">
      <label className="search-field">
        <Search size={15} />
        <input
          aria-label={`Search ${label}`}
          maxLength={200}
          value={search}
          onChange={(event) => onSearch(event.target.value)}
          placeholder={`Search ${label}…`}
        />
      </label>
      {onStatus&&!hideStatus&&<ColumnFilter {...statusColumn(status||"",onStatus,options)}/>}
      {(search || status) && (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            onSearch("");
            onStatus?.("");
          }}
        >
          Clear filters
        </Button>
      )}
    </div>
  );
}
