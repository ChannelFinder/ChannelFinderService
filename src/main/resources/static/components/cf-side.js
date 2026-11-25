import { LitElement, html, nothing } from "lit";
import { PATHS, get } from "../api/api.js";
import { toParams } from "../api/query.js";
import {
  ICONS,
  TAG_COLUMN_PREFIX,
  emit,
  filtered,
  formatNumber,
  renderListsState,
  renderNameFilter,
} from "../shared.js";

const COUNTING = "…";
// A longer list shows this many names, and the rest on request.
const SHOWN_AT_FIRST = 20;
const LABELS = { property: "properties", tag: "tags" };

const columnOf = (kind, { name }) => (kind === "tag" ? TAG_COLUMN_PREFIX + name : name);

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
    // The kinds, `property` and `tag`, whose list is shown in full.
    inFull: { state: true },
    tagsOpen: { state: true },
  };

  constructor() {
    super();
    this.filter = "";
    this.counts = new Map();
    this.inFull = new Set();
    this.tagsOpen = false;
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
    // The tags are folded away until they are asked for: by a click, by a filter, or by a tag
    // that is shown as a column.
    const tagShown = changed.has("columns") && this.columns?.some((c) => c.startsWith(TAG_COLUMN_PREFIX));
    if ((changed.has("filter") && this.filter) || tagShown) this.tagsOpen = true;
  }

  toggleInFull(kind) {
    const inFull = new Set(this.inFull);
    if (!inFull.delete(kind)) inFull.add(kind);
    this.inFull = inFull;
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
    } catch {
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
      ${renderNameFilter(this)}
      ${properties.length
        ? html`<header>
              <h2>Properties</h2>
              <span class="cf-small">Click to show as column</span>
            </header>
            ${this.renderEntries("property", properties)}`
        : nothing}
      ${tags.length
        ? html`<details ?open=${this.tagsOpen} @toggle=${(event) => (this.tagsOpen = event.target.open)}>
            <summary>${ICONS.chevronRight} Tags</summary>
            ${this.renderEntries("tag", tags)}
          </details>`
        : nothing}
      ${properties.length + tags.length ? nothing : html`<p class="cf-small">No property or tag matches.</p>`}
    `;
  }

  renderEntries(kind, entries) {
    const inFull = this.inFull.has(kind);
    // A name that is shown as a column stays in the list, so that it can be clicked away there.
    const shown = entries.filter(
      (entry, index) => inFull || index < SHOWN_AT_FIRST || this.columns?.includes(columnOf(kind, entry)),
    );
    const all = `Show all ${formatNumber(entries.length)} ${LABELS[kind]}`;
    const more = html`<button class="cf-plain cf-more" type="button" @click=${() => this.toggleInFull(kind)}>
      ${inFull ? "Show fewer" : all}
    </button>`;
    return html`<ul>
        ${shown.map((entry) => this.renderEntry(kind, entry))}
      </ul>
      ${entries.length > SHOWN_AT_FIRST ? more : nothing}`;
  }

  renderEntry(kind, entry) {
    const { name, owner } = entry;
    const column = columnOf(kind, entry);
    const shown = this.columns?.includes(column) ?? false;
    const count = this.counts.get(column);
    const counted = typeof count === "number";
    return html`<li>
      <button
        type="button"
        class="cf-plain cf-entry-name"
        title="Owner: ${owner}"
        aria-pressed=${shown}
        @click=${() => this.toggleColumn(column)}
      >
        <span>${shown ? ICONS.check : nothing}</span>
        <span>${name}</span>
      </button>
      <button
        type="button"
        class="cf-btn cf-compact ${count === undefined ? "" : "cf-counted"}"
        title=${counted ? "Matching channels. Click to hide." : "Count the matching channels"}
        aria-label="Count matching channels with ${name}"
        @click=${() => this.toggleCount(kind, name, column)}
      >
        ${counted ? formatNumber(count) : (count ?? "Count")}
      </button>
    </li>`;
  }
}

customElements.define("cf-side", CfSide);
