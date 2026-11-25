import { LitElement, html, nothing } from "lit";
import { PATHS, RESULT_WINDOW, UNREACHABLE_EVENT, get, resourceUrl, setCredentials } from "./api/api.js";
import { toParams } from "./api/query.js";
import {
  ICONS,
  PAGE_SIZES,
  byName,
  defaultColumns,
  formatNumber,
  placeUnderButton,
  plural,
} from "./shared.js";
import "./views/cf-search.js";
import "./components/cf-side.js";
import "./views/cf-manage.js";
import "./components/cf-dialogs.js";

const DEFAULT_PAGE_SIZE = 100;
const TOAST_MS = 2500;
// How often the service is checked while the tab is visible.
const STATUS_INTERVAL_MS = 10_000;
const STATUS_TEXTS = {
  ok: "Connected",
  database: "Database not connected",
  unreachable: "Not reachable",
};

/**
 * The root of the page. It holds the state that the views share: the query, the page of results,
 * the selection and the user. The views get it as properties and report changes as events.
 */
class CfApp extends LitElement {
  static properties = {
    view: { state: true },
    query: { state: true },
    from: { state: true },
    size: { state: true },
    // The columns of the results, or null until the first page has shown which to start with.
    columns: { state: true },
    // Channel name to channel, for the channels that are ticked.
    selection: { state: true },
    user: { state: true },
    // The outcome of the last check of the service, or null before the first one.
    status: { state: true },
    properties: { state: true },
    tags: { state: true },
    listError: { state: true },
    // The channels of the current page and the number of all matches.
    result: { state: true },
    searching: { state: true },
    searchError: { state: true },
    requestUrl: { state: true },
    // What the dialogs show, or null when none is open.
    dialog: { state: true },
    // What the last action did, shown in the view until the next action.
    notice: { state: true },
    // The confirmation of a copy, shown for a moment.
    toast: { state: true },
  };

  constructor() {
    super();
    this.view = "search";
    this.columns = null;
    this.selection = new Map();
    this.user = null;
    this.status = null;
    this.properties = null;
    this.tags = null;
    this.listError = "";
    this.result = null;
    this.searching = false;
    this.searchError = "";
    this.requestUrl = null;
    this.dialog = null;
    this.notice = "";
    this.toast = "";
    // Counts the searches, so that the answer to an earlier one can be told apart.
    this.searches = 0;
    this.listsLoaded = Promise.resolve();
    // A listener or a timer does not wait for what it starts, so an error is reported from here.
    this.onPopState = () => {
      // A link to a place on the page, such as the skip link, also ends up here.
      if (location.search === this.urlSearch) return;
      const query = this.query;
      this.readUrl();
      if (this.query !== query) this.selection = new Map();
      this.search().catch(console.error);
    };
    this.checkStatus = () => {
      if (document.visibilityState === "visible") this.loadStatus().catch(console.error);
    };
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    globalThis.addEventListener("popstate", this.onPopState);
    globalThis.addEventListener(UNREACHABLE_EVENT, this.checkStatus);
    this.statusTimer = setInterval(this.checkStatus, STATUS_INTERVAL_MS);
    this.readUrl();
    this.reload().catch(console.error);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    globalThis.removeEventListener("popstate", this.onPopState);
    globalThis.removeEventListener(UNREACHABLE_EVENT, this.checkStatus);
    clearInterval(this.statusTimer);
  }

  /** The query, the page and the columns are kept in the URL, so that a search can be shared. */
  readUrl() {
    const params = new URLSearchParams(location.search);
    const size = Number(params.get("size"));
    const from = Math.max(0, Number(params.get("from")) || 0);
    this.query = params.get("q") ?? "*";
    // A hand-written address can ask for a size that is not offered, or for a page past what
    // the search endpoint returns.
    this.size = PAGE_SIZES.includes(size) ? size : DEFAULT_PAGE_SIZE;
    this.from = Math.min(from, RESULT_WINDOW - this.size);
    const columns = params.get("cols");
    // Without columns in the URL, they are worked out from the first page of results.
    this.columnsChosen = columns !== null;
    this.columns = this.columnsChosen ? columns.split(",").filter(Boolean) : null;
    this.urlSearch = location.search;
  }

  writeUrl({ push }) {
    const params = new URLSearchParams({ q: this.query, from: this.from, size: this.size });
    if (this.columnsChosen) params.set("cols", this.columns.join(","));
    if (push) history.pushState(null, "", "?" + params);
    else history.replaceState(null, "", "?" + params);
    this.urlSearch = location.search;
  }

  /**
   * Check whether ChannelFinder works as a whole: the service answers, it reports its database
   * as connected, and a count of the channels goes through.
   */
  async loadStatus() {
    // A check that fails reports the service as unreachable, which must not start another check.
    if (this.checkingStatus) return;
    this.checkingStatus = true;
    const status = { state: "unreachable", checked: new Date() };
    try {
      status.info = await get(PATHS.info);
      status.state = "database";
      status.count = Number(await get(PATHS.count));
      if (status.info.elastic?.status === "Connected") status.state = "ok";
    } catch (error) {
      status.error = error.message;
    }
    this.status = status;
    this.checkingStatus = false;
  }

  async loadLists() {
    try {
      const [properties, tags] = await Promise.all([get(PATHS.properties), get(PATHS.tags)]);
      this.properties = properties.toSorted(byName);
      this.tags = tags.toSorted(byName);
      this.listError = "";
    } catch (error) {
      this.listError = error.message;
    }
  }

  async search() {
    const search = ++this.searches;
    const params = toParams(this.query);
    params.set("~size", this.size);
    params.set("~from", this.from);
    params.set("~track_total_hits", "true");
    this.searching = true;
    let result = { channels: [], total: 0 };
    let searchError = "";
    try {
      const data = await get(PATHS.combined, params);
      result = { channels: data.channels, total: data.count };
    } catch (error) {
      searchError = error.message;
    }
    if (!this.columns) await this.listsLoaded;
    // A slower, earlier search must not replace the answer to a later one.
    if (search !== this.searches) return;
    if (!result.channels.length && result.total && this.from) {
      // The page is past the end, for example after channels were deleted.
      this.from = 0;
      this.writeUrl({ push: false });
      await this.search();
      return;
    }
    if (!this.columns && !searchError) this.columns = defaultColumns(this.properties ?? [], result.channels);
    this.result = result;
    this.searchError = searchError;
    this.requestUrl = resourceUrl(PATHS.combined, params);
    this.searching = false;
    this.refreshSelection(result.channels);
  }

  /** Replace the selected channels that are on the page with what the search just returned. */
  refreshSelection(channels) {
    const fresh = channels.filter((channel) => this.selection.has(channel.name));
    if (!fresh.length) return;
    const selection = new Map(this.selection);
    for (const channel of fresh) selection.set(channel.name, channel);
    this.selection = selection;
  }

  /** Read everything on the page again: the state of the service, the lists and the search. */
  async reload() {
    // The search waits for the lists where it needs them, so they are started first.
    this.listsLoaded = this.loadLists();
    await Promise.all([this.loadStatus(), this.listsLoaded, this.search()]);
  }

  showToast(message) {
    this.toast = message;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => (this.toast = ""), TOAST_MS);
  }

  async onQuery(event) {
    // The selection belongs to the query. Searching for the same again keeps it.
    const selected = event.detail === this.query ? 0 : this.selection.size;
    this.notice = "";
    if (selected) {
      this.selection = new Map();
      this.notice = `Selection of ${plural(selected, "channel", "channels")} cleared for the new query.`;
    }
    this.query = event.detail;
    this.from = 0;
    this.writeUrl({ push: true });
    await this.search();
  }

  async onPage(event) {
    this.from = event.detail.from;
    this.size = event.detail.size;
    this.writeUrl({ push: false });
    await this.search();
  }

  onColumns(event) {
    this.columns = event.detail;
    this.columnsChosen = true;
    this.writeUrl({ push: false });
  }

  onSelect(event) {
    this.selection = event.detail;
    this.notice = "";
  }

  onDialog(event) {
    this.dialog = event.detail;
    if (this.dialog) this.notice = "";
  }

  onToast(event) {
    this.showToast(event.detail);
  }

  async onWritten(event) {
    this.user = event.detail.user;
    this.dialog = null;
    if (event.detail.clearSelection) this.selection = new Map();
    this.notice = `${event.detail.message}.`;
    await this.reload();
  }

  forget() {
    setCredentials();
    this.user = null;
    this.notice = "Logged out.";
  }

  /*
   * The views report what the user did as events, which bubble up to `main` below:
   *
   *   cf-query    a new query was entered; the detail is its text
   *   cf-page     another page or page size was chosen; { from, size }
   *   cf-columns  a column was added or hidden; the list of columns
   *   cf-select   rows were ticked or cleared; the new Map of name to channel
   *   cf-dialog   a dialog is to open, or to close with null; { type, ... } as `cf-dialogs` reads it
   *   cf-toast    something was copied; the text to show for a moment
   *   cf-written  a change was made; { user, message, clearSelection }
   *   cf-reload   the data on the page is to be read again; no detail
   */
  render() {
    return html`
      <a class="cf-skip" href="#cf-results">Skip to results</a>
      ${this.renderTop()}
      <main
        @cf-query=${this.onQuery}
        @cf-page=${this.onPage}
        @cf-columns=${this.onColumns}
        @cf-select=${this.onSelect}
        @cf-dialog=${this.onDialog}
        @cf-toast=${this.onToast}
        @cf-written=${this.onWritten}
        @cf-reload=${this.reload}
      >
        <cf-search
          ?hidden=${this.view !== "search"}
          .query=${this.query}
          .from=${this.from}
          .size=${this.size}
          .columns=${this.columns}
          .selection=${this.selection}
          .result=${this.result}
          .searching=${this.searching}
          .searchError=${this.searchError}
          .requestUrl=${this.requestUrl}
          .properties=${this.properties}
          .tags=${this.tags}
          .listError=${this.listError}
          .notice=${this.notice}
        ></cf-search>
        <cf-manage
          ?hidden=${this.view !== "manage"}
          .properties=${this.properties}
          .tags=${this.tags}
          .listError=${this.listError}
          .notice=${this.notice}
        ></cf-manage>
        <cf-dialogs
          .spec=${this.dialog}
          .user=${this.user}
          .selection=${this.selection}
          .properties=${this.properties ?? []}
          .tags=${this.tags ?? []}
        ></cf-dialogs>
      </main>
      <div class="cf-toast" role="status">${this.toast}</div>
    `;
  }

  renderViewButton(view, label) {
    return html`<button
      class="cf-plain"
      type="button"
      aria-current=${this.view === view ? "page" : nothing}
      @click=${() => (this.view = view)}
    >
      ${label}
    </button>`;
  }

  renderTop() {
    const version = this.status?.info?.version;
    return html`<header class="cf-top">
      <div class="cf-brand">
        <h1>ChannelFinder</h1>
        <span class="cf-small">${version}</span>
      </div>
      <nav class="cf-views" aria-label="Views">
        ${this.renderViewButton("search", "Search")} ${this.renderViewButton("manage", "Manage")}
      </nav>
      <span id="cf-status" role="status">${this.renderStatus()}</span>
      <nav class="cf-links" aria-label="Documentation and source">
        <a href="https://channelfinder.readthedocs.io/en/latest/">Docs${ICONS.external}</a>
        <a href="swagger-ui/index.html">API</a>
        <a href="https://github.com/ChannelFinder/ChannelFinderService">Source${ICONS.external}</a>
      </nav>
      ${this.user
        ? html`<span>
            Changes sent as <b>${this.user}</b>
            <button class="cf-btn cf-compact" type="button" @click=${this.forget}>Forget</button>
          </span>`
        : nothing}
    </header>`;
  }

  /** The state of the service and the number of channels. A click on the state opens the details. */
  renderStatus() {
    const { state, count } = this.status ?? {};
    const total = html`<span><span id="cf-total">${formatNumber(count)}</span> channels</span>`;
    return html`<button
        class="cf-plain cf-status"
        type="button"
        popovertarget="cf-status-details"
        data-state=${state ?? "checking"}
      >
        ${STATUS_TEXTS[state] ?? "Checking…"}
      </button>
      <div id="cf-status-details" class="cf-popover cf-dropdown" popover @beforetoggle=${placeUnderButton}>
        ${this.renderStatusDetails()}
      </div>
      ${count === undefined ? nothing : total}`;
  }

  renderStatusDetails() {
    if (!this.status) return html`<p>Checking…</p>`;
    const { info, checked, error } = this.status;
    const elastic = info?.elastic ?? {};
    // A line is left empty, and so not shown, where the service did not say.
    const database = info ? `Database: ${elastic.status ?? "not known"}` : "";
    const service = info ? `${info.name} ${info.version}` : "";
    const search = elastic.version ? `Elasticsearch ${elastic.version}` : "";
    const cluster = elastic.clusterName ? `Cluster ${elastic.clusterName}` : "";
    return html`<p>Last checked at ${checked.toLocaleTimeString()}</p>
      <p>${database}</p>
      <p class="cf-error">${error}</p>
      <hr />
      <p>${service}</p>
      <p>${search}</p>
      <p>${cluster}</p>`;
  }
}

customElements.define("cf-app", CfApp);
