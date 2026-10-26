# LOG-A-TRON interface

The Angular application uses the same component structure in light and dark modes. The saved `logatron.theme` preference controls `data-theme` and PrimeNG's `.app-dark` selector. The root component initializes the preference before loading a route, including the development login screen.

## Styling ownership

Styles load in this order in `frontend/angular.json`:

1. PrimeIcons.
2. `design-tokens.css`: semantic palettes, typography, spacing, radii, shadows, focus, and motion tokens.
3. `styles.css`: shared controls, PrimeNG integration, application shell, scope picker, tables, feedback, and accessible detail panels.
4. `observability.css`: overview metrics and charts, log search/context/live states, trace lookup, and waterfall.
5. `investigation.css`: investigation input, findings, failure timeline, dependency flow, evidence, provider boundary, and verification steps.
6. `platform.css`: project catalog/topology, administration, confirmation dialog, login, and restricted access.

The four former override sheets (`theme-light.css`, `role-ux.css`, `visual-fixes.css`, and `managed-onboarding.css`) have been consolidated into these owners. Theme-specific colors belong in tokens rather than route selectors. `ObservabilityPreset` sets the PrimeNG blue action palette; native and PrimeNG controls share semantic surfaces and text colors.

## Route and interaction coverage

| Route | Page-specific design |
| --- | --- |
| `/login` | Branded sign-in panel, identity selector, telemetry capabilities, theme control, and retained development-only issuer warning. |
| `/overview` | Four metrics, true error ratio, error chart with numeric scale, service ranking using catalog names, correlation coverage, and empty/error/loading states. |
| `/logs` | Scope and search controls, expandable identifiers, saved-search controls and feedback, severity table, technical text, bounded live-tail statuses, and pagination. |
| `/logs?eventId=…` | Authorized event-detail panel with message/stack trace, safe attributes, identifiers, log context, and trace/investigation pivots. |
| `/logs` context state | Explicit anchor and before/after markers, counts, and return to search. |
| `/logs` live-tail state | Connecting, connected, paused, failed/reconnect, and bounded stream termination states with actual streaming status. |
| `/traces` | Trace-ID validation, focused lookup form, and instrumentation guidance. |
| `/traces/:traceId` | Summary metrics, timing ruler, scrollable hierarchy and waterfall, error spans, detail panel, and related-log/investigation actions. |
| `/investigations` | Start/loading/result states, root-cause summary, chronological chain, affected services, dependency flow, bounded provider, evidence grid, metric provenance panel, and evidence-quality warnings. |
| `/projects` | Authorized project cards with classification, actual topology counts, status, and explore action. |
| `/projects/:id` | Hierarchy path, deployment selectors, selectable instances, source mappings, logical services, and catalog loading/error/empty states. |
| `/administration` | Section navigation, onboarding progression, labeled forms, environments, services, servers, instances, collectors, revision history, sources, memberships, safe secret dismissal, and native decommission confirmation. |
| `/access-denied` | Branded restricted-access message and safe return navigation. |

## Behavior and accessibility

- Backend APIs, authorization guards, capability-derived navigation, redaction, and bounded query models remain authoritative.
- Scope restoration validates requested IDs against returned catalog options. A request generation prevents a delayed parent-scope response from repopulating a newer selection. Service changes immediately update instance options.
- Saved searches restore the displayed scope, identifiers, severity, and supported relative time window.
- Log evidence links load their referenced event through the existing detail API. Metric evidence opens the returned bounded reference/provenance; it does not invent a chart or add a metrics API.
- Live tail becomes connected only after a streaming response is established. Closing, pausing, failing, or reaching a reported duration limit removes the active-stream state. An aborted older stream cannot overwrite the next connection.
- The API indicator describes the most recent API response, with an initial unknown state and an unavailable state for transport/server errors.
- Detail panels contain Tab focus, close with Escape, and restore focus to their opener. Native confirmation dialogs retain explicit cancellation and confirmation. Controls have visible focus treatment and decorative icons are excluded from native button names. PrimeNG buttons have explicit accessible labels.
- Desktop navigation collapses; mobile navigation opens over the content. Tables and waterfalls scroll within their containers. Evidence, filters, metrics, and forms adapt to narrower widths. Reduced-motion preferences disable decorative motion.

## Verification

From `frontend/`:

```powershell
pnpm install --frozen-lockfile
pnpm build
pnpm test --watch=false
```

Focused regression tests cover theme persistence, API status, saved-filter restoration, authorized scope restoration and stale requests, event evidence links, live-tail termination/races, and decommission safeguards. Existing authentication, navigation, capability, and scope tests remain in place.

Browser review uses the repository's real evaluation API and sanctioned log generator. No production request paths are replaced with screenshot fixtures. Generated screenshots and verification manifests belong under ignored `output/ui-redesign/`, outside production source. Temporary review scripts and the isolated Compose override belong under ignored `tmp/`.

Review light and dark modes at 1920, 1440, 1280, 1024, 768, and 390 pixels. Check populated and empty results, drawers, confirmations, validation feedback, failed requests, and mobile navigation as well as default route layouts. Collector screenshots must dismiss the one-time secret before capture. A registered but uninstalled review collector correctly remains offline/pending; the interface must not claim it is healthy.
