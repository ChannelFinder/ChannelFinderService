# ChannelFinder API design docs

Internal design notes for the ChannelFinder REST API — not user-facing reference
(that is the live [Swagger UI](http://localhost:8080/swagger-ui/index.html) and
the [ReadTheDocs site](https://channelfinder.readthedocs.io/en/latest/)).

## Contents

| File              | What it is                                                                                  |
| ----------------- | ------------------------------------------------------------------------------------------ |
| `migrations.md`   | v0's problems and the v0 → v1 decisions, each as *v0 / v1 / why*.                            |
| `conventions.md`  | The v1 REST reference — resources, methods, status codes, naming, auth, querying/RSQL, pagination, jobs, channel state, reconcile, sources, processors, observability, v0 coexistence, and per-resource endpoint tables. |

`migrations.md` is the *why* (v0's problems and the v1 decisions); `conventions.md`
is the *what* (the v1 rules and endpoint reference). Each reads on its own.

## The domain in one paragraph

ChannelFinder is a directory of **channels**. A channel has a unique `name`, a
CF-owned lifecycle `state`, a set of **properties** (name-value pairs, each value
a string, an integer, or a float), a set of **tags** (names), and the set of **sources** that
claim it — a PV served by more than one IOC has more than one claim. Properties and tags are defined independently and then attached to
channels. Clients query for channels matching property/tag/name expressions.
Alongside the three domain resources are three operational resources — jobs
(handles on async work), processors (channel post-processors), and sources
(records of which producer claims what) — plus the out-of-band Actuator surface
for service info, health, and metrics. See the
top-level [README](../../README.md) for what ChannelFinder is and how to run it.

## Use cases

Who uses ChannelFinder and how — what the design is answerable to:

| Actor                                        | Access               | Auth                          | Cadence / scale                                  |
| -------------------------------------------- | -------------------- | ----------------------------- | ------------------------------------------------ |
| RecCeiver / channel producer (a `source`)    | write                | `channel` (+`property`/`tag`) | Bursty: thousands of channels per IOC start/stop |
| Phoebus & other read clients                 | read                 | none                          | Interactive and frequent                         |
| Metadata curator / admin                     | write                | `tag`, `property`, `admin`    | Occasional, deliberate                           |
| Bundled admin web UI (`static/`)             | read + write         | `channel`, `tag`, `property`  | Ad-hoc, browser, same-origin                     |
| Channel processors (e.g. Archiver Appliance) | internal side effect | n/a (server-side)             | Automatic on every create/update                 |
| Operators / monitoring                       | read                 | none                          | Continuous scrape                                |

How those patterns shape the API:

- **Bulk-first writes.** Producers push thousands of channels per IOC, so a whole scope is one `POST /channels:reconcile` command that CF diffs server-side. An oversized one runs as an async job.
- **Producers are sources with a lifecycle.** Each producer is a `source` that claims the channels it serves; a PV served by several IOCs has several claims, and CF owns each channel's `state` as the OR of them. Claims are inspectable at `/sources`, and retiring a decommissioned producer is `DELETE /sources/{source_id}`.
- **Open, search-centric reads.** Discovery is unauthenticated and query-dominated, so v0's three paging/count surfaces collapse into one `GET /channels`, with richer filtering deferred to RSQL.
- **Definitions gate attachments.** Properties and tags are defined independently and then attached, so they are both top-level resources and channel sub-resources, and attaching an undefined one is a `422`.
- **Metadata drives side effects.** Channel properties (`archive`, `archiver`) and lifecycle `state` trigger processors, so a run is an explicit `POST /channels:process` command and processors are inspectable via `/processors`.
- **Some work outlives a request.** A directory-wide processor run or an oversized bulk write returns a job the client polls.

## Status

These docs describe a target, not a frozen contract. The codebase is structured
for versioned controllers (`web/v0/...`), leaving room to add `v1` alongside
without breaking existing clients.
