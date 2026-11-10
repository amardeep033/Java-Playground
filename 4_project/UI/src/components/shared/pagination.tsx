import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
export function Pagination({ page, totalPages, totalElements, onPage }: {
  page: number; totalPages: number; totalElements: number; onPage: (page: number) => void;
}) {
  return <nav className="pagination" aria-label="Pagination">
    <span>{totalElements} {totalElements === 1 ? "record" : "records"} <span className="muted">· Page {Math.min(page+1,Math.max(1,totalPages))} of {Math.max(1,totalPages)}</span></span>
    <Button variant="outline" size="sm" disabled={page===0} onClick={()=>onPage(page-1)} aria-label="Previous page"><ChevronLeft size={15} />Previous</Button>
    <Button variant="outline" size="sm" disabled={page+1>=totalPages} onClick={()=>onPage(page+1)} aria-label="Next page">Next<ChevronRight size={15} /></Button>
  </nav>;
}
