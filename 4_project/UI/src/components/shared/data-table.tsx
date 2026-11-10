import {ColumnFilter,type ColumnFilterProps} from "./column-filter";
import type { ReactNode } from "react";
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
} from "@tanstack/react-table";
import { Pagination } from "./pagination";
import type { Page } from "@/types/api";
import { EmptyState } from "./feedback";
export function DataTable<T>({
  data,
  columns,
  page,
  onPage,
  emptyTitle,
  emptyDescription,
  onInspect,
  inspectLabel="View details",
  keepHeaders=false,
  renderHeader,
  columnFilters,
}: {
  data: Page<T>;
  columns: ColumnDef<T>[];
  page: number;
  onPage: (page: number) => void;
  emptyTitle: string;
  emptyDescription: string;
  onInspect?: (row:T)=>void;
  inspectLabel?:string;
  keepHeaders?:boolean;
  renderHeader?:(id:string)=>ReactNode;
  columnFilters?:Record<string,ColumnFilterProps>;
}) {
  const displayColumns=[...columns];
  if(columnFilters?.status&&!columns.some(column=>"accessorKey" in column&&column.accessorKey==="status"))displayColumns.splice(Math.max(1,columns.length-1),0,{
    id:"status",header:"Status",accessorFn:(row)=>{const item=row as {active?:boolean;revokedAt?:string|null};return "active" in item?(item.active?"Active":"Inactive"):item.revokedAt?"Revoked":"Active";},cell:({getValue})=><span className="badge">{getValue<string>()}</span>
  });
  const table = useReactTable({
    data: data.items,
    columns:displayColumns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
  });
  return (
    <div className="table-card">
      {data.totalElements === 0 && !keepHeaders && !columnFilters ? <div className="table-empty"><EmptyState title={emptyTitle} description={emptyDescription} /></div> : <div className="table-scroll">
        <table>
          <thead>
            {table.getHeaderGroups().map((group) => (
              <tr key={group.id}>
                {group.headers.map((header) => (
                  <th key={header.id} scope="col">
                    {columnFilters?.[header.column.id] ? <ColumnFilter {...columnFilters[header.column.id]}/> : renderHeader ? renderHeader(header.column.id) : flexRender(
                      header.column.columnDef.header,
                      header.getContext(),
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {(keepHeaders||!!columnFilters)&&data.items.length===0&&<tr><td colSpan={displayColumns.length}><EmptyState title={emptyTitle} description={emptyDescription}/></td></tr>}
            {table.getRowModel().rows.map((row) => (
              <tr key={row.id} className={[onInspect?"inspect-row":"",new URLSearchParams(window.location.search).get("selected")===(row.original as {id?:string}).id?"selected-row":""].join(" ")}
                onClick={event=>{
                  // React portal events bubble through their source row; dialogs are not row clicks.
                  if(!event.currentTarget.contains(event.target as Node))return;
                  if((event.target as HTMLElement).closest("button,a,input,select,textarea,summary,label"))return;
                  if(onInspect)onInspect(row.original);
                  else event.currentTarget.querySelector<HTMLButtonElement>("button.resource-link")?.click();
                }}>

                {row.getVisibleCells().map((cell,index) => (
                  <td key={cell.id} className={onInspect&&index===0?"inspect-cell":undefined}>
                    {onInspect&&index===0?<button type="button" className="cell-trigger resource-link" aria-label={inspectLabel} onClick={()=>onInspect(row.original)}>{flexRender(cell.column.columnDef.cell,cell.getContext())}</button>:flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>}
      <Pagination page={page} totalPages={data.totalPages} totalElements={data.totalElements} onPage={onPage} />
    </div>
  );
}
