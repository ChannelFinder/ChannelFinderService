/** Names and helpers that more than one view uses. */
import { html, svg } from "./vendor/lit-core.min.js";

export const PAGE_SIZES = [50, 100, 250, 500, 1000];
// A column of the results is a property name, the list of all tags, or one tag.
export const TAGS_COLUMN = "~tags";
export const TAG_COLUMN_PREFIX = "~tag:";
// The properties that recCeiver writes. They are shown first where the service has them.
const PREFERRED_COLUMNS = ["iocName", "pvStatus", "hostName", "recordType", "recordDesc"];
const DEFAULT_PROPERTY_COLUMNS = 4;

const NUMBER = new Intl.NumberFormat("en-GB");

export const formatNumber = (n) => NUMBER.format(n);
export const plural = (n, one, many) => `${formatNumber(n)} ${n === 1 ? one : many}`;
// Numbers inside a name sort by value, so `prop9` comes before `prop10`.
const NAME_ORDER = new Intl.Collator("en", { numeric: true, sensitivity: "base" });
export const byName = (a, b) => NAME_ORDER.compare(a.name, b.name);

// One set of icons: strokes on a grid of 24 by 24, in the colour of the text around them.
const icon = (shapes) => html`<svg class="cf-icon" viewBox="0 0 24 24" aria-hidden="true">${shapes}</svg>`;
export const ICONS = {
  check: icon(svg`<path d="M20 6 9 17l-5-5" />`),
  chevronDown: icon(svg`<path d="m6 9 6 6 6-6" />`),
  chevronLeft: icon(svg`<path d="m15 18-6-6 6-6" />`),
  chevronRight: icon(svg`<path d="m9 18 6-6-6-6" />`),
  copy: icon(svg`<rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15V5a2 2 0 0 1 2-2h10" />`),
  external: icon(svg`<path d="M7 17 17 7M8 7h9v9" />`),
  help: icon(svg`<circle cx="12" cy="12" r="9" /><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3M12 17h.01" />`),
  plus: icon(svg`<path d="M12 5v14M5 12h14" />`),
  search: icon(svg`<circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />`),
  trash: icon(svg`<path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v6M14 11v6" />`),
  x: icon(svg`<path d="M18 6 6 18M6 6l12 12" />`),
};

export function emit(from, type, detail) {
  from.dispatchEvent(new CustomEvent(type, { detail, bubbles: true }));
}

/**
 * Place a popover under the button that opens it, as a drop-down. This is the `beforetoggle`
 * handler of the popover. Left to itself, the browser puts a popover in the middle of the page.
 */
export function placeUnderButton(event) {
  if (event.newState !== "open") return;
  const popover = event.target;
  const opener = document.querySelector(`[popovertarget="${CSS.escape(popover.id)}"]`);
  const button = opener.getBoundingClientRect();
  popover.style.top = `${button.bottom + 2}px`;
  // Line up with the edge of the button that is further from the edge of the window.
  if (button.left < window.innerWidth / 2) popover.style.left = `${button.left}px`;
  else popover.style.right = `${window.innerWidth - button.right}px`;
}

/** Return the columns to show before the user has chosen any. */
export function defaultColumns(properties, channels) {
  const known = new Set(properties.map((property) => property.name));
  const columns = PREFERRED_COLUMNS.filter((name) => known.has(name)).slice(0, DEFAULT_PROPERTY_COLUMNS);
  if (columns.length < DEFAULT_PROPERTY_COLUMNS) {
    // Fill up with the properties that most channels on the first page have.
    const uses = new Map();
    for (const channel of channels) {
      for (const { name } of channel.properties) uses.set(name, (uses.get(name) ?? 0) + 1);
    }
    const mostUsed = [...uses.keys()]
      .filter((name) => !columns.includes(name))
      .toSorted((a, b) => uses.get(b) - uses.get(a) || a.localeCompare(b));
    columns.push(...mostUsed.slice(0, DEFAULT_PROPERTY_COLUMNS - columns.length));
  }
  return [...columns, TAGS_COLUMN];
}

/**
 * Return what a view shows in place of the properties and tags while they are not there, or
 * nothing once they are.
 */
export function renderListsState(view) {
  if (view.listError) {
    return html`<p class="cf-error">
      Could not load the properties and tags. ${view.listError}
      <button class="cf-btn" type="button" @click=${() => emit(view, "cf-reload")}>Retry</button>
    </p>`;
  }
  if (!view.properties || !view.tags) return html`<p class="cf-small">Loading…</p>`;
  return undefined;
}

/** The box that narrows the properties and tags of a view to the names with its text in them. */
export function renderNameFilter(view) {
  return html`<label class="cf-field">
    ${ICONS.search}
    <input
      type="text"
      placeholder="Filter properties and tags"
      aria-label="Filter properties and tags"
      .value=${view.filter}
      @input=${(event) => (view.filter = event.target.value)}
    />
    <button
      class="cf-plain cf-icon-btn"
      type="button"
      aria-label="Clear the filter"
      ?hidden=${!view.filter}
      @click=${(event) => {
        view.filter = "";
        event.currentTarget.previousElementSibling.focus();
      }}
    >
      ${ICONS.x}
    </button>
  </label>`;
}

/** Return the entries whose names have the text of the filter box of a view in them. */
export function filtered(view, entries) {
  const wanted = view.filter.trim().toLowerCase();
  return entries.filter((entry) => entry.name.toLowerCase().includes(wanted));
}
