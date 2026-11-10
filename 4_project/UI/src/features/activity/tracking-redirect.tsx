import { Navigate, useParams, useLocation } from "react-router-dom";
import type { TrackingKind } from "./tracking-context";
/** Existing bookmarks open the same inspector over the corresponding list. */
export function TrackingRedirect({kind,id,section}:{kind:TrackingKind;id:string;section:string}) {
 const {userId,appId}=useParams();const location=useLocation();const params=new URLSearchParams(location.search);params.set("inspect",kind);params.set("inspectId",id);
 return <Navigate to={`/users/${userId}/apps/${appId}/${section}?${params}`} replace/>;
}
