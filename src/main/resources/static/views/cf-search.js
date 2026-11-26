import { LitElement, html, nothing } from "lit";
import { PATHS, RESULT_WINDOW, resourceUrl } from "../api/api.js";
import { queryHint } from "../api/query.js";
import { deleteChannels } from "../api/writes.js";
import {
  ICONS,
  PAGE_SIZES,
  TAGS_COLUMN,
  TAG_COLUMN_PREFIX,
  byName,
  emit,
  formatNumber,
  placeUnderButton,
} from "../shared.js";

/** @typedef {import("../api/query.js").Channel} Channel */

const DELETE_CHANNELS = "delete-channels";
// More channels than this do not fit side by side.
const COMPARE_AT_MOST = 10;
// Column widths in rem: the width of a character, the room for padding and buttons, and the limits.
const SELECT_WIDTH = 2.8;
const NAME_WIDTH = { character: 0.66, extra: 6.8, most: 22 };
const VALUE_WIDTH = { character: 0.56, extra: 2.8, least: 5, most: 16 };
// A cell shows this many tags, and the number of the others.
const TAGS_SHOWN = 3;
const TAGS_WIDTH_MOST = 26;
// The columns get this much of the table in per cent. The rest covers the column of tick boxes.
const SHARED_PERCENT = 96;

function columnLabel(column) {
  if (column === TAGS_COLUMN) return "Tags";
  return column.startsWith(TAG_COLUMN_PREFIX) ? column.slice(TAG_COLUMN_PREFIX.length) : column;
}

const isTagColumn = (column) => column === TAGS_COLUMN || column.startsWith(TAG_COLUMN_PREFIX);

/** Return the tags of a cell: all tags of the channel, or the one tag of the column if it has it. */
function cellTags(channel, column) {
  const tags = channel.tags.map((tag) => tag.name);
  return column === TAGS_COLUMN ? tags : tags.filter((tag) => tag === columnLabel(column));
}

/** Return the text of a cell that is not for tags: the value of a property. */
function cellText(channel, column) {
  return channel.properties.find((property) => property.name === column)?.value ?? "";
}

/** Return a text that is about as wide as the tags of a cell are when shown. */
function tagsWidthText(tags) {
  const shown = tags.slice(0, TAGS_SHOWN);
  if (tags.length > TAGS_SHOWN) shown.push("+99");
  // Three characters stand for the padding of a tag and the gap to the next.
  return shown.join("   ");
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
    const ofTags = isTagColumn(column);
    const cells = channels.map((channel) =>
      ofTags ? tagsWidthText(cellTags(channel, column)) : cellText(channel, column),
    );
    const wanted = longest([columnLabel(column), ...cells]) * VALUE_WIDTH.character + VALUE_WIDTH.extra;
    return Math.max(VALUE_WIDTH.least, Math.min(wanted, ofTags ? TAGS_WIDTH_MOST : VALUE_WIDTH.most));
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

  clearDraft() {
    this.draft = "";
    this.querySelector("#cf-query").focus();
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
    if (event.target.closest("a, button, input, label")) return;
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
          <label class="cf-field">
            ${ICONS.search}
            <input
              type="text"
              id="cf-query"
              class="cf-mono"
              aria-label="Channel query"
              autocomplete="off"
              spellcheck="false"
              placeholder="name*  tag  property=value"
              .value=${this.draft}
              @input=${(event) => (this.draft = event.target.value)}
            />
            <button
              class="cf-plain cf-icon-btn"
              type="button"
              aria-label="Clear the query"
              ?hidden=${!this.draft}
              @click=${this.clearDraft}
            >
              ${ICONS.x}
            </button>
            <button
              class="cf-plain cf-icon-btn"
              type="button"
              aria-label="Query syntax"
              popovertarget="cf-syntax"
            >
              ${ICONS.help}
            </button>
          </label>
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
    if (!url) return html``;
    // Decoded so that it can be read, apart from spaces, which would look like the end of a value.
    const readable = decodeURIComponent(url.pathname + url.search).replaceAll(" ", "%20");
    return html`<p class="cf-small cf-request cf-mono">
      <span>GET</span>
      <a href=${url.href} target="_blank" rel="noopener" title="Open the raw API answer in a new tab"
        >${readable}</a
      >
      <button
        class="cf-plain cf-icon-btn"
        type="button"
        aria-label="Copy the request URL"
        @click=${() => this.copy(url.href, "URL copied")}
      >
        ${ICONS.copy}
      </button>
    </p>`;
  }

  renderCount() {
    if (!this.result) return html`Searching…`;
    const { total } = this.result;
    const limit = total > RESULT_WINDOW ? `Paging stops at ${formatNumber(RESULT_WINDOW)}.` : "";
    return html`<span ?hidden=${!total}>
      <b>${formatNumber(total)}</b> ${total === 1 ? "channel" : "channels"}
      <span class="cf-small">${limit}</span>
    </span>`;
  }

  renderToolbar() {
    const selected = this.selection.size;
    const tooMany = selected > COMPARE_AT_MOST ? `Compare takes at most ${COMPARE_AT_MOST}.` : "";
    return html`<div class="cf-toolbar">
      <span id="cf-count" role="status">${this.renderCount()}</span>
      ${selected ? this.renderSelection(selected) : nothing}
      <span class="cf-grow cf-small" role="status">${this.notice} ${tooMany}</span>
      ${this.renderPager()}
    </div>`;
  }

  /** What can be done with the ticked channels. It is shown only while some are ticked. */
  renderSelection(selected) {
    return html`<span class="cf-selection">
      <b>${formatNumber(selected)} selected</b>
      <button
        class="cf-btn"
        type="button"
        ?disabled=${selected < 2 || selected > COMPARE_AT_MOST}
        @click=${() => emit(this, "cf-dialog", { type: "compare" })}
      >
        Compare
      </button>
      <button class="cf-btn" type="button" popovertarget="cf-edit-menu">Edit ${ICONS.chevronDown}</button>
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
      <button class="cf-plain" type="button" @click=${() => emit(this, "cf-select", new Map())}>Clear</button>
    </span>`;
  }

  /** The page size, the rows that are shown, and the page, within what the search endpoint returns. */
  renderPager() {
    const { channels = [], total = 0 } = this.result ?? {};
    const last = Math.min(total, RESULT_WINDOW);
    const range = `${formatNumber(this.from + 1)}–${formatNumber(this.from + channels.length)}`;
    return html`<span class="cf-pager cf-small">
      <label>
        Rows
        <select @change=${(event) => this.page(0, Number(event.target.value))}>
          ${PAGE_SIZES.map((size) => html`<option ?selected=${size === this.size}>${size}</option>`)}
        </select>
      </label>
      <span id="cf-range" ?hidden=${!channels.length}>${range} of ${formatNumber(total)}</span>
      <button
        class="cf-btn cf-icon-btn"
        type="button"
        aria-label="Previous page"
        ?disabled=${this.from === 0}
        @click=${() => this.page(Math.max(0, this.from - this.size))}
      >
        ${ICONS.chevronLeft}
      </button>
      <button
        class="cf-btn cf-icon-btn"
        type="button"
        aria-label="Next page"
        ?disabled=${this.from + this.size >= last}
        @click=${() => this.page(this.from + this.size)}
      >
        ${ICONS.chevronRight}
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
    // The columns share the width of the table in proportion to what each wants. The table is
    // never narrower than they want, and scrolls inside its box where it does not fit.
    const wanted = this.widths.reduce((sum, width) => sum + width, 0);
    const share = (width) => ((width / wanted) * SHARED_PERCENT).toFixed(2);
    return html`<table
      class="cf-table cf-grid"
      style="min-inline-size:${wanted.toFixed(1)}rem"
    >
      <colgroup>
        <col style="width:${SELECT_WIDTH}rem" />
        ${this.widths.map((width) => html`<col style="width:${share(width)}%" />`)}
        <col />
      </colgroup>
      <thead>
        <tr>
          <th>
            <label class="cf-tick">
              <input
                type="checkbox"
                aria-label="Select all on this page"
                .checked=${allSelected}
                .indeterminate=${selectedHere > 0 && !allSelected}
                @change=${(event) => this.select(channels, event.target.checked)}
              />
            </label>
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
        class="cf-plain cf-icon-btn"
        type="button"
        aria-label="Hide column ${label}"
        @click=${() => this.hideColumn(column)}
      >
        ${ICONS.x}
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
          <label class="cf-tick">
            <input
              type="checkbox"
              aria-label="Select ${name}"
              .checked=${selected}
              @change=${(event) => this.select([channel], event.target.checked)}
            />
          </label>
        </td>
        <td>
          <div class="cf-name">
            <button
              type="button"
              class="cf-plain cf-icon-btn cf-chevron"
              aria-expanded=${open}
              aria-label="Details of ${name}"
              @click=${() => this.toggleOpen(name)}
            >
              ${ICONS.chevronRight}
            </button>
            <span class="cf-mono" title=${name}>${name}</span>
            <button
              type="button"
              class="cf-plain cf-icon-btn cf-copy"
              aria-label="Copy name"
              @click=${() => this.copy(name, "Name copied")}
            >
              ${ICONS.copy}
            </button>
          </div>
        </td>
        ${columns.map((column) => this.renderCell(channel, column))}
        <td></td>
      </tr>
      ${open ? this.renderDetails(channel, columns.length + 3) : nothing}`;
  }

  /** @param {Channel} channel */
  renderCell(channel, column) {
    if (!isTagColumn(column)) {
      const text = cellText(channel, column);
      return html`<td title=${text}>${text}</td>`;
    }
    const tags = cellTags(channel, column);
    const more = tags.length - TAGS_SHOWN;
    return html`<td title=${tags.join(" ")}>
      <span class="cf-tags">
        ${tags.slice(0, TAGS_SHOWN).map((tag) => html`<span>${tag}</span>`)}
        ${more > 0 ? html`<i>+${formatNumber(more)}</i>` : nothing}
      </span>
    </td>`;
  }

  /** @param {Channel} channel */
  renderDetails(channel, span) {
    const properties = channel.properties.toSorted(byName);
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
