import { LitElement, html, nothing } from "./vendor/lit-core.min.js";
import { PATHS, RESULT_WINDOW, resourceUrl } from "./api.js";
import { queryHint } from "./query.js";
import { deleteChannels } from "./writes.js";
import {
  PAGE_SIZES,
  TAGS_COLUMN,
  TAG_COLUMN_PREFIX,
  byName,
  emit,
  formatNumber,
  placeUnderButton,
  plural,
} from "./shared.js";

/** @typedef {import("./query.js").Channel} Channel */

const DELETE_CHANNELS = "delete-channels";
// More channels than this do not fit side by side.
const COMPARE_AT_MOST = 10;
// Column widths in rem: the width of a character, the room for padding and buttons, and the limits.
const SELECT_WIDTH = 2.2;
const NAME_WIDTH = { character: 0.66, extra: 5.2, most: 22 };
const VALUE_WIDTH = { character: 0.56, extra: 2.8, least: 5, most: 16 };
// The columns get this much of the table in per cent. The rest covers the column of tick boxes.
const SHARED_PERCENT = 96;
// How far the columns may be narrowed together before the table scrolls sideways.
const LEAST_PART = 0.8;

const ICON_COPY = html`<svg class="cf-icon" viewBox="0 0 16 16" aria-hidden="true">
  <rect x="5" y="5" width="9" height="9" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.4" />
  <path d="M3 11V3.5A1.5 1.5 0 0 1 4.5 2H11" fill="none" stroke="currentColor" stroke-width="1.4" />
</svg>`;
const ICON_CHEVRON = html`<svg class="cf-icon" viewBox="0 0 10 10" aria-hidden="true">
  <path d="M3 1.5 6.5 5 3 8.5" fill="none" stroke="currentColor" stroke-width="1.6" />
</svg>`;

function columnLabel(column) {
  if (column === TAGS_COLUMN) return "Tags";
  return column.startsWith(TAG_COLUMN_PREFIX) ? column.slice(TAG_COLUMN_PREFIX.length) : column;
}

/** Return the text of a cell: a property value, all tags, or one tag where the channel has it. */
function cellText(channel, column) {
  const tags = channel.tags.map((tag) => tag.name);
  if (column === TAGS_COLUMN) return tags.join(" ");
  if (column.startsWith(TAG_COLUMN_PREFIX))
    return tags.includes(columnLabel(column)) ? columnLabel(column) : "";
  return channel.properties.find((property) => property.name === column)?.value ?? "";
}

/**
 * Return the width of the name column and of each other column, from the longest text in each.
 * This is done once for a page of results, so that the columns stay put while it is shown.
 *
 * @param {Channel[]} channels
 */
function columnWidths(channels, columns) {
  const longest = (texts) => Math.max(0, ...texts.map((text) => text.length));
  const names = channels.map((channel) => channel.name);
  const name = Math.min(longest(names) * NAME_WIDTH.character, NAME_WIDTH.most) + NAME_WIDTH.extra;
  const values = columns.map((column) => {
    const texts = [columnLabel(column), ...channels.map((channel) => cellText(channel, column))];
    const wanted = longest(texts) * VALUE_WIDTH.character + VALUE_WIDTH.extra;
    return Math.max(VALUE_WIDTH.least, Math.min(wanted, VALUE_WIDTH.most));
  });
  return [name, ...values];
}

/** The search view: the query box, the list of properties and tags, and the table of results. */
class CfSearch extends LitElement {
  static properties = {
    query: { attribute: false },
    from: { attribute: false },
    size: { attribute: false },
    columns: { attribute: false },
    selection: { attribute: false },
    result: { attribute: false },
    searching: { attribute: false },
    searchError: { attribute: false },
    requestUrl: { attribute: false },
    properties: { attribute: false },
    tags: { attribute: false },
    listError: { attribute: false },
    notice: { attribute: false },
    // The text in the query box, which differs from `query` while the user types.
    draft: { state: true },
    // The names of the channels whose details are shown.
    open: { state: true },
  };

  constructor() {
    super();
    this.draft = "";
    this.open = new Set();
  }

  createRenderRoot() {
    return this;
  }

  willUpdate(changed) {
    if (changed.has("query")) this.draft = this.query;
    if (changed.has("result") || changed.has("columns")) {
      this.widths = columnWidths(this.result?.channels ?? [], this.columns ?? []);
    }
  }

  firstUpdated() {
    this.querySelector("#cf-query").focus();
  }

  submit(event) {
    event.preventDefault();
    emit(this, "cf-query", this.draft.trim() || "*");
  }

  page(from, size = this.size) {
    emit(this, "cf-page", { from, size });
  }

  hideColumn(column) {
    const columns = this.columns.filter((shown) => shown !== column);
    emit(this, "cf-columns", columns);
  }

  /** @param {Channel[]} channels */
  select(channels, selected) {
    const selection = new Map(this.selection);
    for (const channel of channels) {
      if (selected) selection.set(channel.name, channel);
      else selection.delete(channel.name);
    }
    emit(this, "cf-select", selection);
  }

  toggleOpen(name) {
    const open = new Set(this.open);
    if (!open.delete(name)) open.add(name);
    this.open = open;
  }

  /** A click anywhere on a row opens its details, unless it was on a control or a link. */
  onRowClick(event, name) {
    if (event.target.closest("a, button, input")) return;
    this.toggleOpen(name);
  }

  async copy(text, done) {
    try {
      await navigator.clipboard.writeText(text);
      emit(this, "cf-toast", done);
    } catch {
      emit(this, "cf-toast", "The browser blocked copying");
    }
  }

  /**
   * Start the edit chosen in the menu. Deleting needs no further choice, so it goes straight to
   * the dialog that confirms it.
   */
  edit(event) {
    const kind = event.target.value;
    if (!kind) return;
    event.currentTarget.hidePopover();
    if (kind === DELETE_CHANNELS)
      emit(this, "cf-dialog", { type: "write", ...deleteChannels(this.selection) });
    else emit(this, "cf-dialog", { type: "edit", kind });
  }

  render() {
    return html`
      <cf-side
        .query=${this.query}
        .columns=${this.columns}
        .properties=${this.properties}
        .tags=${this.tags}
        .listError=${this.listError}
      ></cf-side>
      <section class="cf-pane">
        <form class="cf-query" @submit=${this.submit}>
          <input
            type="search"
            id="cf-query"
            class="cf-mono"
            aria-label="Channel query"
            autocomplete="off"
            spellcheck="false"
            placeholder="name*  tag  property=value"
            .value=${this.draft}
            @input=${(event) => (this.draft = event.target.value)}
          />
          <button class="cf-btn" type="button" popovertarget="cf-syntax">Syntax</button>
          <button class="cf-btn cf-primary">Search</button>
        </form>
        ${this.renderSyntax()}
        <p class="cf-small">${this.properties ? queryHint(this.query, this.properties) : nothing}</p>
        ${this.renderRequest(this.requestUrl)} ${this.renderToolbar()}
        <div
          class="cf-results"
          id="cf-results"
          tabindex="-1"
          role="region"
          aria-label="Search results"
          ?inert=${this.searching}
        >
          ${this.renderResults()}
        </div>
      </section>
    `;
  }

  renderSyntax() {
    const term = (example, meaning) =>
      html`<dt>${example}</dt>
        <dd>${meaning}</dd>`;
    return html`<div id="cf-syntax" class="cf-popover" popover>
      <h2>Query syntax</h2>
      <dl class="cf-syntax">
        ${term(
          "ABC:*:00?",
          "Name pattern, the first word. * is any run of characters and ? is one. Case is ignored.",
        )}
        ${term("pvStatus=Active", "Property value. A query may start with this; the name pattern is then *.")}
        ${term('recordDesc="Water flow*"', "Quote a value that has spaces in it.")}
        ${term("ABC:*|XYZ:*", "A | or a comma means OR inside one value.")}
        ${term("alias=*", "Has the property, with any value.")}
        ${term("alias!=*", "Does not have the property.")}
        ${term("~tag=archived", "Has the tag. A bare word after the name pattern means the same.")}
        ${term("~tag!=archived", "Does not have the tag.")}
      </dl>
      <p class="cf-small">
        For anything else, such as more than ${formatNumber(RESULT_WINDOW)} results, use the
        <a href="swagger-ui/index.html">REST API</a>.
      </p>
    </div>`;
  }

  /** A request as a link to the raw answer of the API, for a search or for one channel. */
  renderRequest(url) {
    if (!url) return nothing;
    // Decoded so that it can be read, apart from spaces, which would look like the end of a value.
    const readable = decodeURIComponent(url.pathname + url.search).replaceAll(" ", "%20");
    return html`<p class="cf-small cf-request">
      Request:
      <a
        class="cf-mono"
        href=${url.href}
        target="_blank"
        rel="noopener"
        title="Open the raw API answer in a new tab"
        >GET ${readable}</a
      >
    </p>`;
  }

  renderCount() {
    if (!this.result) return "Searching…";
    const { channels, total } = this.result;
    if (!total) return nothing;
    const range = `${formatNumber(this.from + 1)}–${formatNumber(this.from + channels.length)}`;
    return html`Showing <b>${range}</b> of <b>${plural(total, "channel", "channels")}</b> ${total >
      RESULT_WINDOW
        ? html`<span class="cf-small">Paging stops at ${formatNumber(RESULT_WINDOW)}.</span>`
        : nothing}`;
  }

  renderToolbar() {
    const selected = this.selection.size;
    return html`<div class="cf-toolbar">
      <span id="cf-count" role="status">${this.renderCount()}</span>
      <span class="cf-grow cf-small" role="status">${this.notice}</span>
      ${selected
        ? html`<span>
            ${formatNumber(selected)}
            selected${selected > COMPARE_AT_MOST ? `; Compare takes at most ${COMPARE_AT_MOST}` : nothing}
            <button
              class="cf-btn cf-compact"
              type="button"
              @click=${() => emit(this, "cf-select", new Map())}
            >
              Clear
            </button>
          </span>`
        : nothing}
      <button
        class="cf-btn"
        type="button"
        ?disabled=${selected < 2 || selected > COMPARE_AT_MOST}
        @click=${() => emit(this, "cf-dialog", { type: "compare" })}
      >
        Compare
      </button>
      <button class="cf-btn" type="button" popovertarget="cf-edit-menu" ?disabled=${!selected}>
        Edit <span aria-hidden="true">▾</span>
      </button>
      <div
        id="cf-edit-menu"
        class="cf-popover cf-dropdown cf-menu"
        popover
        @beforetoggle=${placeUnderButton}
        @click=${this.edit}
      >
        <button class="cf-plain" type="button" value="add-tag">Add tag</button>
        <button class="cf-plain" type="button" value="remove-tag">Remove tag</button>
        <button class="cf-plain" type="button" value="set-property">Set property</button>
        <button class="cf-plain" type="button" value="remove-property">Remove property</button>
        <button class="cf-plain" type="button" value=${DELETE_CHANNELS}>Delete channels</button>
      </div>
      ${this.renderPager()}
    </div>`;
  }

  /** The page size and the page, within what the search endpoint returns. */
  renderPager() {
    const total = Math.min(this.result?.total ?? 0, RESULT_WINDOW);
    return html`<span class="cf-pager">
      <label class="cf-small">
        Rows
        <select @change=${(event) => this.page(0, Number(event.target.value))}>
          ${PAGE_SIZES.map((size) => html`<option ?selected=${size === this.size}>${size}</option>`)}
        </select>
      </label>
      <button
        class="cf-btn"
        type="button"
        aria-label="Previous page"
        ?disabled=${this.from === 0}
        @click=${() => this.page(Math.max(0, this.from - this.size))}
      >
        ‹
      </button>
      <button
        class="cf-btn"
        type="button"
        aria-label="Next page"
        ?disabled=${this.from + this.size >= total}
        @click=${() => this.page(this.from + this.size)}
      >
        ›
      </button>
    </span>`;
  }

  renderResults() {
    if (this.searchError) {
      return html`<p class="cf-error">
        The search failed. ${this.searchError}
        <button class="cf-btn" type="button" @click=${() => emit(this, "cf-reload")}>Retry</button>
      </p>`;
    }
    if (!this.result) return html`<p class="cf-small">Searching…</p>`;
    const { channels } = this.result;
    if (!channels.length) {
      return html`<p>
        No channels match <code>${this.query}</code>. Check the spelling, or remove a filter.
      </p>`;
    }
    const columns = this.columns ?? [];
    const selectedHere = channels.filter((channel) => this.selection.has(channel.name)).length;
    const allSelected = selectedHere === channels.length;
    // The columns share the width of the table in proportion to what each wants. The table gives
    // way down to a part of the wanted width, and scrolls inside its box below that.
    const wanted = this.widths.reduce((sum, width) => sum + width, 0);
    const share = (width) => ((width / wanted) * SHARED_PERCENT).toFixed(2);
    return html`<table
      class="cf-table cf-grid"
      style="min-inline-size:${(wanted * LEAST_PART).toFixed(1)}rem"
    >
      <colgroup>
        <col style="width:${SELECT_WIDTH}rem" />
        ${this.widths.map((width) => html`<col style="width:${share(width)}%" />`)}
        <col />
      </colgroup>
      <thead>
        <tr>
          <th>
            <input
              type="checkbox"
              aria-label="Select all on this page"
              .checked=${allSelected}
              .indeterminate=${selectedHere > 0 && !allSelected}
              @change=${(event) => this.select(channels, event.target.checked)}
            />
          </th>
          <th>Name</th>
          ${columns.map((column) => this.renderHeader(column))}
          <th></th>
        </tr>
      </thead>
      <tbody>
        ${channels.map((channel) => this.renderRow(channel, columns))}
      </tbody>
    </table>`;
  }

  renderHeader(column) {
    const label = columnLabel(column);
    return html`<th title=${label}>
      ${label}
      <button
        class="cf-plain"
        type="button"
        aria-label="Hide column ${label}"
        @click=${() => this.hideColumn(column)}
      >
        ✕
      </button>
    </th>`;
  }

  /** @param {Channel} channel */
  renderRow(channel, columns) {
    const { name } = channel;
    const open = this.open.has(name);
    const selected = this.selection.has(name);
    return html`<tr
        class="cf-row ${open ? "cf-open" : ""} ${selected ? "cf-selected" : ""}"
        @click=${(event) => this.onRowClick(event, name)}
      >
        <td>
          <input
            type="checkbox"
            aria-label="Select ${name}"
            .checked=${selected}
            @change=${(event) => this.select([channel], event.target.checked)}
          />
        </td>
        <td>
          <div class="cf-name">
            <button
              type="button"
              class="cf-plain cf-chevron"
              aria-expanded=${open}
              aria-label="Details of ${name}"
              @click=${() => this.toggleOpen(name)}
            >
              ${ICON_CHEVRON}
            </button>
            <span class="cf-mono" title=${name}>${name}</span>
            <button
              type="button"
              class="cf-plain cf-copy"
              aria-label="Copy name"
              @click=${() => this.copy(name, "Name copied")}
            >
              ${ICON_COPY}
            </button>
          </div>
        </td>
        ${columns.map((column) => {
          const text = cellText(channel, column);
          return html`<td title=${text}>${text}</td>`;
        })}
        <td></td>
      </tr>
      ${open ? this.renderDetails(channel, columns.length + 3) : nothing}`;
  }

  /** @param {Channel} channel */
  renderDetails(channel, span) {
    const properties = [...channel.properties].sort(byName);
    return html`<tr class="cf-details">
      <td colspan=${span}>
        <div class="cf-details-body">
          <dl>
            ${properties.map(
              ({ name, value }) =>
                html`<div>
                  <dt title=${name}>${name}</dt>
                  <dd>${value}</dd>
                </div>`,
            )}
            <div>
              <dt>tags</dt>
              <dd class="cf-tags">
                ${channel.tags.map((tag) => html`<span>${tag.name}</span>`)}
                ${channel.tags.length ? nothing : "none"}
              </dd>
            </div>
            <div>
              <dt>owner</dt>
              <dd>${channel.owner}</dd>
            </div>
          </dl>
          ${this.renderRequest(resourceUrl([...PATHS.channels, channel.name]))}
        </div>
      </td>
    </tr>`;
  }
}

customElements.define("cf-search", CfSearch);
