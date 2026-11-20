import { LitElement, html, nothing } from "./vendor/lit-core.min.js";
import { PATHS, get } from "./api.js";
import { toParams } from "./query.js";
import {
  TAG_COLUMN_PREFIX,
  emit,
  filtered,
  formatNumber,
  renderListsState,
  renderNameFilter,
} from "./shared.js";

const COUNTING = "…";

/**
 * The list of properties and tags next to the results. A name can be shown as a column, and
 * counted among the current matches.
 */
class CfSide extends LitElement {
  static properties = {
    query: { attribute: false },
    columns: { attribute: false },
    properties: { attribute: false },
    tags: { attribute: false },
    listError: { attribute: false },
    filter: { state: true },
    // Column id to the number of matches that have it, or to a mark while counting.
    counts: { state: true },
  };

  constructor() {
    super();
    this.filter = "";
    this.counts = new Map();
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this.role = "complementary";
    this.ariaLabel = "Properties and tags";
  }

  willUpdate(changed) {
    // A count belongs to the query it was made for.
    if (changed.has("query")) this.counts = new Map();
  }

  toggleColumn(column) {
    const columns = this.columns ?? [];
    const shown = columns.includes(column);
    emit(this, "cf-columns", shown ? columns.filter((other) => other !== column) : [...columns, column]);
  }

  /**
   * Show how many of the current matches have a tag or a property, or hide that number again.
   * Each count is one request.
   */
  async toggleCount(kind, name, column) {
    const shown = this.counts.get(column);
    if (shown === COUNTING) return;
    if (shown !== undefined) {
      const counts = new Map(this.counts);
      counts.delete(column);
      this.counts = counts;
      return;
    }
    const query = this.query;
    const params = toParams(query);
    if (kind === "tag") params.append("~tag", name);
    else params.append(name, "*");
    this.counts = new Map(this.counts).set(column, COUNTING);
    let count;
    try {
      count = Number(await get(PATHS.count, params));
    } catch (error) {
      count = "failed";
    }
    if (query === this.query) this.counts = new Map(this.counts).set(column, count);
  }

  render() {
    const state = renderListsState(this);
    if (state) return state;
    const properties = filtered(this, this.properties);
    const tags = filtered(this, this.tags);
    return html`
      <p class="cf-small">Click a name to add a column.</p>
      ${renderNameFilter(this)} ${this.renderEntries("Properties", "property", properties)}
      ${this.renderEntries("Tags", "tag", tags)}
      ${properties.length + tags.length ? nothing : html`<p class="cf-small">No property or tag matches.</p>`}
    `;
  }

  renderEntries(title, kind, entries) {
    if (!entries.length) return nothing;
    return html`<h2>${title}</h2>
      <ul>
        ${entries.map((entry) => this.renderEntry(kind, entry))}
      </ul>`;
  }

  renderEntry(kind, { name, owner }) {
    const column = kind === "tag" ? TAG_COLUMN_PREFIX + name : name;
    const shown = this.columns?.includes(column) ?? false;
    const count = this.counts.get(column) ?? "count";
    return html`<li>
      <button
        type="button"
        class="cf-plain cf-entry-name"
        title="Owner: ${owner}"
        aria-pressed=${shown}
        @click=${() => this.toggleColumn(column)}
      >
        ${name}
      </button>
      <button
        type="button"
        class="cf-btn cf-compact"
        title=${typeof count === "number"
          ? "Matching channels. Click to hide."
          : "Count the matching channels"}
        aria-label="Count matching channels with ${name}"
        @click=${() => this.toggleCount(kind, name, column)}
      >
        ${typeof count === "number" ? formatNumber(count) : count}
      </button>
    </li>`;
  }
}

customElements.define("cf-side", CfSide);
