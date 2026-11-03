import { lazy, Suspense } from "react";
import {
  createBrowserRouter,
  Navigate,
  Link,
  useRouteError,
} from "react-router-dom";
import { Loading } from "@/components/shared/feedback";
import { Button } from "@/components/ui/button";
const Welcome = lazy(() => import("@/features/auth/welcome-page"));
const Layout = lazy(() => import("@/app/layouts/workspace-layout"));
const Applications = lazy(
  () => import("@/features/applications/applications-page"),
);
const Overview = lazy(() => import("@/features/applications/overview-page"));
const Events = lazy(() => import("@/features/events/event-types-page"));
const Endpoints = lazy(() => import("@/features/endpoints/endpoints-page"));
const Keys = lazy(() => import("@/features/api-keys/api-keys-page"));
const ActivityEvents = lazy(() => import("@/features/activity/events-page"));
const EventDetails = lazy(
  () => import("@/features/activity/event-detail-page"),
);
const Deliveries = lazy(() => import("@/features/activity/deliveries-page"));
const DeliveryDetails = lazy(
  () => import("@/features/activity/delivery-detail-page"),
);
const Settings = lazy(() => import("@/features/settings/settings-page"));
const load = (element: React.ReactNode) => (
  <Suspense fallback={<Loading />}>{element}</Suspense>
);
function RouteError() {
  const error = useRouteError();
  return (
    <main className="standalone-error">
      <h1>We couldn’t open this page.</h1>
      <p>
        {error instanceof Error ? error.message : "The page may have moved."}
      </p>
      <Button asChild>
        <Link to="/">Back to Event Relay</Link>
      </Button>
    </main>
  );
}
export const router = createBrowserRouter([
  { path: "/", element: load(<Welcome />), errorElement: <RouteError /> },
  {
    path: "/users/:userId",
    element: load(<Layout />),
    errorElement: <RouteError />,
    children: [
      { index: true, element: <Navigate to="applications" replace /> },
      { path: "applications", element: load(<Applications />) },
      { path: "settings", element: load(<Settings />) },
      { path: "apps/:appId/overview", element: load(<Overview />) },
      { path: "apps/:appId/events", element: load(<ActivityEvents />) },
      { path: "apps/:appId/events/:eventId", element: load(<EventDetails />) },
      { path: "apps/:appId/deliveries", element: load(<Deliveries />) },
      {
        path: "apps/:appId/deliveries/:deliveryId",
        element: load(<DeliveryDetails />),
      },
      {
        path: "apps/:appId/dead-letters",
        element: load(<Deliveries failed />),
      },
      { path: "apps/:appId/event-types", element: load(<Events />) },
      { path: "apps/:appId/endpoints", element: load(<Endpoints />) },
      { path: "apps/:appId/api-keys", element: load(<Keys />) },
    ],
  },
  {
    path: "*",
    element: (
      <main className="standalone-error">
        <h1>Page not found</h1>
        <Button asChild>
          <Link to="/">Back to Event Relay</Link>
        </Button>
      </main>
    ),
  },
]);
