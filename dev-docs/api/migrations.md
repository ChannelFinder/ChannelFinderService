# Migration: v0 → v1

Why today's ChannelFinder API (the live `web/v0/controller` classes) is being
reshaped into a more RESTful surface: the problems it leaves behind and the
decision behind each change, so they need not be re-litigated. v1 is the target
direction; the codebase already lives under `web/v0/...`, leaving room for a `v1`
package alongside it without breaking existing clients.

## Legacy problems to avoid

| Issue                        | Examples / pattern                                                                                                          |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Inverted method semantics    | `PUT` creates (`create`, `create_1`…`create_5`), `POST` updates — the reverse of REST norms.                               |
| RPC-style paths              | `PUT /resources/processors/process/{query,channels,all}` — verbs in the path, not resources + methods.                     |
| Non-descriptive operationIds | Generated, collision-suffixed: `list`, `list_1`, `read`, `read_1`, `query_1`, `create_1`…`create_5`.                       |
| Untyped schemas              | Bodies typed as bare `type: object`; responses served as `*/*` instead of `application/json`.                              |
| Three ways to page/count     | `/scroll` + `/scroll/{scrollId}`, `/channels/count`, and `/channels/combined` all coexist.                                 |
| No error contract            | No error schema; failures are unmodelled, so clients cannot rely on a shape.                                               |
| List vs detail entangled     | `Tag` embeds a full `channels[]`; a `withChannels` boolean toggles whether it is populated.                               |
| Request = response schema    | One DTO both directions (`PropertyDto`/`Channel`), so write bodies carry read-only fields (`PropertyDto.channels`) the server ignores. |
| Properties as a list         | A channel's properties are `List<{name, owner, value}>` (`entity/Channel.java`): scan to read one, duplicates allowed, `owner` repeated per entry. |
| Untyped values               | Every property value is a string; numeric fields (position, sector) sort lexically, worked around by zero-padding.          |
| Definitions write channels   | `PUT /properties/{propertyName}/{channelName}`, `POST /properties/{propertyName}`, and matching `DELETE` mutate channel membership from the definition side. |
| Casing                       | `camelCase` params (`tagName`, `channelName`, `withChannels`) and the `/ChannelFinder/resources/...` prefix on every path. |
| Opaque query input           | Search is a single required `allRequestParams` `MultiValueMap` blob, not named parameters.                                 |
| Boolean logic in magic chars | AND/OR/NOT encoded in punctuation — `!` negate, `\|,;` OR, distinct params AND — an undocumented DSL (`ChannelRepository.getBuiltQuery`). |
| Long work blocks the caller  | Bulk writes and `/processors/process/all` run inside the request; no handle to observe, resume, or survive a timeout.       |
| Clients compute lifecycle    | A producer must read CF and diff it against its own set to find what went away, then write the deactivation itself.         |
| Lifecycle is arbitrary metadata | "Channel is inactive" is an ordinary property (`pvStatus`), hand-editable, with no record of when it changed or who decided it. |
| Single source per name       | CF stores one host's metadata per channel name; a second IOC serving the same PV silently clobbers the first, with no record. |
| Processors as RPC + flag     | `GET /processors/count`, `PUT /processors/process/*` (run), and `PUT /processor/{name}/enabled` (config) tangled on one controller. |
| Bespoke service-info endpoint | `GET /ChannelFinder` returns status from a hand-rolled `InfoController` instead of Spring Boot Actuator (already on the classpath). |

## Design decisions and rationale

**Version the API and drop the service prefix.**
- *v0:* every path carries `/ChannelFinder/resources/…`, and nothing is versioned.
- *v1:* `/channels` under a `v1` base; the prefix is gone.
- *Why:* the prefix is redundant noise, and a version lets v1 ship alongside v0 without breaking existing clients.

**HTTP methods mean what HTTP says.**
- *v0:* `PUT` creates (`create`, `create_1`…`create_5`) and `POST` updates — the reverse of REST norms.
- *v1:* `POST` creates (`201` + `Location`), `PUT` replaces, `PATCH` partial-updates, `GET` reads, `DELETE` removes (`204`).
- *Why:* least surprise, and standard tooling, caches, and proxies can rely on method semantics.

**One wire vocabulary: `snake_case`.**
- *v0:* `camelCase` params (`channelName`, `withChannels`) mixed with the `/ChannelFinder/resources/…` path prefix.
- *v1:* `snake_case` in paths, query params, and JSON alike (`channel_name`, `total_count`, `next_cursor`, `source_id`).
- *Why:* one casing everywhere removes a per-field decision and the mismatch between URL and body.

**A real error contract.**
- *v0:* no error schema; failures are unmodelled, so clients cannot rely on a shape.
- *v1:* RFC 9457 Problem Details (`application/problem+json`), distinguishing `400` (syntax) from `422` (business-rule).
- *Why:* a stable, typed failure shape clients can parse.

**Stable operationIds and typed schemas.**
- *v0:* generated, collision-suffixed ids (`list`, `list_1`, `create_1`…`create_5`); bodies typed as bare `type: object`; responses served as `*/*`.
- *v1:* a semantic `{verb}{Resource}` id and a named schema per operation; `application/json` responses; nullables as `oneOf` until an OAS-3.1 generator (3.0 renders array-form nullables as `any`).
- *Why:* usable generated clients, and stable ids that map to controller methods.

**Three resources unchanged.**
- *v1:* channels, properties, and tags stay three resources; properties/tags are defined independently and attached to a channel with a per-channel value (properties) or a bare label (tags).
- *Why:* the domain model is unchanged — only the HTTP surface is reshaped.

**Attachment is a sub-resource, not a two-segment path.**
- *v0:* `PUT /tags/{tagName}/{channelName}` puts the child's identity in the parent's path.
- *v1:* `PUT /channels/{channel_name}/tags/{tag_name}`.
- *Why:* reads the way the relationship is owned — a channel has tags — and stays within two levels of nesting.

**Add vs. replace is explicit per method.**
- *v1:* on a channel's property/tag list, `PUT` replaces the whole list; `PATCH` adds or updates without removing the unmentioned.
- *Why:* makes the bulk-writer's intent (push a full set vs. patch a few) explicit instead of inferred from payload shape.

**Bulk writes become one declarative reconcile.**
- *v0:* a producer's push is several separate calls with no shared outcome, buildable only by reading CF first — what exists decides create vs. update, what CF holds decides what to deactivate.
- *v1:* `POST /channels:reconcile` takes the producer's current set and CF computes the difference; plain methods stay for single-resource writes.
- *Why:* bundling the calls would keep that read, just earlier — so invert it and let CF diff.

**Channel lifecycle is derived from source claims.**
- *v0:* `pvStatus` (Active/Inactive) is an ordinary property written by the producer, so RecCeiver must read CF and diff it to find what went away. The Archiver Appliance processor already pauses archiving on `Inactive` (`AAChannelProcessor`).
- *v1:* a channel has no lifecycle field of its own — it is `active` while any source claims it and `inactive` once the last claim drops. No client writes it; it follows from the source links, each carrying its own `updated_at`. The `pvStatus` name is reserved: a v0 write of it is applied as a change to that producer's claim. Cutover is per-source.
- *Why:* lifecycle is a fact about who is still serving the channel, not a value to hand-edit — deriving it from the claims removes the producer-writes-then-CF-trusts step and makes `inactive` mean exactly "no source claims it". An `inactive` channel is soft-deleted (record stays; the next reconcile that mentions it revives it), so reads default-exclude `inactive` (`state=inactive`, `state=any` to include) — the directory reads clean while history survives.

**Reconcile is scoped and ordered.**
- *v1:* a submission carries `source_id` (scope) and `generation` (order); CF deactivates the in-scope channels a submission omits. `generation` is a wall clock, compared only within one `source_id`. `last_seen_at` lives on the source record, not the channel. A v0 producer that sets neither still lands in a stable scope: CF derives `source_id` from the request host and reads a v0 `iocid` property as `source_id`.
- *Why:* deleting is safe only if the server knows what a producer answers for; a counter would reset on restart and make every later submission look stale; a per-channel last-seen stamp would rewrite every channel on every restart, whereas `updated_at` is last-*modified* and moves only on a real change.

**Reconcile guards against accidental mass deactivation.**
- *v0:* an empty `channels[]` deactivates the entire scope — and a producer that starts before its configuration loads sends exactly that, in earnest.
- *v1:* an empty desired set, or one that would deactivate more than a configured fraction of the scope, is rejected (`422`, the count named in the body) unless the request carries an explicit confirmation flag.
- *Why:* the guard is on reconcile, where the empty set is an accident; the deliberate `DELETE /sources/{source_id}` deactivates a whole scope too but is admin-gated and intentional, so it needs no flag.

**One channel, many sources.**
- *v0:* CF stores one host's metadata per name; a second IOC serving the same PV silently clobbers the first, with no record.
- *v1:* keep `name` as the key and hang a `sources[]` array off the channel, one entry per claiming source. Existence reference-counts: `active` while any claim is active, `inactive` once the last drops. Shared metadata still resolves by last-write, but CF sets a `conflict` flag instead of clobbering silently.
- *Why:* readers resolve a PV by name and expect one answer; a composite key would push disambiguation onto Phoebus and the archiver. Additive — a new field, not a reshaped key. Per-source metadata was considered and dropped — it makes the flattened read arbitrary for a case the flag already surfaces.

**Purge is configuration.**
- *v1:* the retention window is a site setting, not a number fixed in the contract; `state` and `updated_at` make "everything inactive since before X" a simple query.
- *Why:* soft delete accumulates records, so something must hard-delete them eventually; how much dead history is worth keeping is a site question, not an API one.

**Retiring a source is an explicit scope delete.**
- *v0:* a `source_id` survives restarts, so a decommissioned IOC leaves a claim nobody ever drops — and deactivation only ever comes from a source reconciling *without* a channel.
- *v1:* the record is readable at `GET /sources` / `GET /sources/{source_id}`, and `DELETE /sources/{source_id}` flips every one of the source's links to `active: false`; the purge lifecycle does the rest — channels now inactive on all their sources purge, a channel another source still claims stays alive, and the source record follows once its channels are gone.
- *Why:* a dead source never reconciles again, so retirement must be explicit rather than timed; admin-only and deliberate, so no confirmation flag.

**Scroll → opaque cursor, not a server-held session.**
- *v0:* leaks Elasticsearch's `scroll`/`scrollId` into the URL, making an engine detail the public contract.
- *v1:* offset (`size` + `from`) for browsing plus an opaque `cursor` the backend maps to whatever the engine uses for full traversal; count folds into the response (`total_count`), so `scroll`, `/channels/count`, and `/channels/combined` collapse into one `GET /channels` returning `{ total_count, channels[], next_cursor? }`.
- *Why:* `scroll` is deprecated upstream (in favour of `search_after` + a point-in-time id), and reviving a server-held session would re-adopt the stateful model being retired.

**`withChannels` toggle → split representations.**
- *v0:* one schema whose heavy `channels[]` field is conditionally populated by a `withChannels` boolean.
- *v1:* separate list and detail projections.
- *Why:* clients get a predictable shape and collection reads stay lean.

**Request and response are separate schemas.**
- *v0:* one DTO both directions, so a write body carries read-only fields the server ignores.
- *v1:* each direction its own schema — a write body holds only what the caller may set, the response holds the full read projection.

**Properties are a typed name→value map, not a list.**
- *v0:* `List<{name, owner, value}>` forces a scan to read one, permits duplicate names, and types every value as a string — so numeric fields sort lexically and sites zero-pad (sample data stores `cell`/`family` as `%03d`).
- *v1:* serialize properties as a map (`{ "device": "bpm", "cell": 2, "position": 187.28 }`) and tags as a name list. A value is string / integer / float, fixed at the property's definition, so numeric fields sort and range numerically instead of lexically. 
- *Why:* JSON separates text from numeric; the definition separates integer from float. Three types match the domain's real shapes (categorical text, integer indices like `cell`, continuous measures like `position`) while keeping the mapping closed. The map makes lookup direct and removes duplicate-name ambiguity by construction.

**Property types**
- *v0:* property values are strings, numeric fields sort lexically and sites zero-pad.
- *v1:* property values are strings or numeric (integer or float). Creation of a property passes an optional `type` field to specify the value type (string, integer, float).
- *Why:* Allow sorting and range queries on numeric fields.

**Attachment is written only through the channel.**
- *v0:* channel membership can be written from the definition side — a second write path parallel to writing the channel.
- *v1:* `/properties` and `/tags` define and read the vocabulary; membership is written only through the channel sub-resources.
- *Why:* one fact, one write path.

**Long-running work becomes a job.**
- *v0:* unbounded operations (a `/processors/process/all` sweep, a huge reconcile) run inside the request — timeouts with no "still going" status, no handle to observe or cancel, duplicate work when a timed-out caller retries.
- *v1:* `202 Accepted` and a `/jobs/{job_id}` resource the client polls (non-terminal `status`, `progress`, per-item `result`). Process commands are always async; reconciles opt in with `Prefer: respond-async`.
- *Why:* retry duplication is handled per command (a reconcile's `generation` identifies the submission; a repeated sweep is harmless), not by an `Idempotency-Key`.

**Richer queries use RSQL.**
- *v0:* an implicit boolean DSL — `!` negate, `|,;` OR, distinct-params AND — undocumented punctuation.
- *v1:* the first surface is simple AND-of-equality/glob; negation, OR, and grouped precedence move to RSQL in a single `filter` parameter (a strict superset of v0, standard grammar, mature libraries). Both surfaces namespace selectors the same way — channel fields (`name`, `tag`, `state`, `source_id`) reserved, property selectors prefixed `prop_`; absence (`prop_cell!=*`) stays distinct from value (`prop_cell!=2`).
- *Why:* rather than carry undocumented punctuation forward. Alternatives considered: OData, a JSON:API filter profile, GraphQL.

**Owner drops off the v1 surface.**
- *v0:* records an `owner` on every resource but never enforces it — writes gate on role alone.
- *v1:* omit `owner` from requests and responses; authorization stays role-based (`channel`/`property`/`tag`/`admin`). v0 clients that still send or read `owner` keep working.
- *Why:* don't carry a field the API does not use. Owner-scoped authorization can be added later, re-entering the surface deliberately.

**Auth transport: Basic and Bearer.**
- *v0:* authenticates writes with HTTP Basic only.
- *v1:* keep Basic for compatibility and add Bearer (service tokens recommended for machine producers like RecCeiver); exactly one scheme per request, never both. Reads stay unauthenticated.
- *Why:* v1 preserves v0's method-based access split — `GET` open, writes authenticated and role-gated (`channel`/`property`/`tag`/`admin`) — and makes the failure explicit (`401` for a missing/invalid credential, `403` for a valid one without the role).

**Aggregating property values.**
- *v0:* reads are channel-centric, so "what values does `ioc_id` take, and how common is each?" means paging the whole result and counting client-side.
- *v1:* `GET /properties/{property_name}/values` returns distinct values and per-value channel counts, scoped by the same query as `GET /channels`.
- *Why:* stays behind an opaque contract, so the engine that computes it can change without breaking clients.

**Processors: a resource and a command.**
- *v0:* inspecting, running, and configuring processors are tangled on one controller via RPC verb paths and a boolean-flag sub-path.
- *v1:* running is the side-effecting `POST /channels:process` command (always async, unbounded by directory size); inspecting and configuring is the `/processors` resource (`GET` read, `PATCH /processors/{name}` for config, chiefly `{ "enabled": false }`).
- *Why:* split definition from attachment as properties/tags do; `enabled` is state on the noun, so it is a `PATCH`, not a `PUT .../enabled` flag path.

**Application info lives in Actuator.**
- *v0:* a hand-rolled `GET /ChannelFinder` (`InfoController`).
- *v1:* Spring Boot Actuator (already enabled) serves `/actuator/{health,info,metrics,prometheus}` — service metadata and version to `/actuator/info`, liveness/backend status to `/actuator/health`.
- *Why:* these are not domain resources and the standard operational surface already exists. The one consumer to migrate is the admin UI's "Service Info" panel (`cfmanage.js`), repointed at Actuator; rewiring its complicated v0 queries is out of scope.
