import {useEffect,useRef,useState} from "react";
import {ListFilter} from "lucide-react";
export interface FilterOption {value:string;label:string}
export interface ColumnFilterProps {label:string;options?:FilterOption[];selected?:string[];onChange?:(values:string[])=>void;text?:string;onText?:(value:string)=>void;expanded?:boolean;onExpanded?:(open:boolean)=>void;single?:boolean}
export function ColumnFilter({label,options=[],selected=[],onChange,text,onText,expanded,onExpanded,single=false}:ColumnFilterProps) {
 const ref=useRef<HTMLDetailsElement>(null);const [search,setSearch]=useState("");
 useEffect(()=>{
  const close=(event:PointerEvent)=>{if(ref.current&&!ref.current.contains(event.target as Node)){ref.current.open=false;}};
  const escape=(event:KeyboardEvent)=>{if(event.key==="Escape"&&ref.current?.open){event.preventDefault();event.stopPropagation();ref.current.open=false;ref.current.querySelector("summary")?.focus();}};
  document.addEventListener("pointerdown",close);window.addEventListener("keydown",escape,true);
  return()=>{document.removeEventListener("pointerdown",close);window.removeEventListener("keydown",escape,true);};
 },[]);
 return <details ref={ref} className="column-filter" open={expanded} onToggle={event=>onExpanded?.(event.currentTarget.open)}>
  <summary aria-label={`Filter ${label}`}>{label}<ListFilter size={14}/>{(selected.length>0||!!text)&&<span className="filter-count">{selected.length||1}</span>}</summary>
  <div className="column-filter-options">
   <input className="input" aria-label={`Search ${label} options`} placeholder={onText?`Search ${label.toLowerCase()}…`:"Search…"} value={onText?text||"":search} onChange={event=>onText?onText(event.target.value):setSearch(event.target.value)}/>
   <button type="button" className="filter-clear" onClick={()=>{onChange?.([]);onText?.("");}}>Clear selection</button>
   {options.filter(option=>option.label.toLowerCase().includes(search.toLowerCase())).map(option=><label key={option.value}><input type="checkbox" checked={selected.includes(option.value)} onChange={event=>onChange?.(event.target.checked?(single?[option.value]:[...selected,option.value]):selected.filter(value=>value!==option.value))}/>{option.label}</label>)}
  </div>
 </details>;
}
export const statusOptions=[{value:"ACTIVE",label:"Active"},{value:"INACTIVE",label:"Inactive"}];
export function statusColumn(status:string,onChange:(value:string)=>void,options=statusOptions):ColumnFilterProps{return {label:"Status",options,selected:status?status.split(","):[],onChange:values=>onChange(values.join(","))};}
