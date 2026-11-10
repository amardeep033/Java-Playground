import { createContext, useContext } from "react";
export type TrackingKind="incoming"|"event"|"deliveries"|"delivery"|"dead-letters"|"dead-letter";
export interface TrackingView {kind:TrackingKind;id:string}
export interface TrackingActions {
 open:(view:TrackingView)=>void;close:()=>void;jump:(view:TrackingView)=>void;
}
export const TrackingContext=createContext<TrackingActions|null>(null);
export function useTracking() {const value=useContext(TrackingContext);if(!value)throw new Error("Tracking provider required");return value;}
