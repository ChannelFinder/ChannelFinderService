/** Names and helpers that more than one view uses. */
import { html } from "./vendor/lit-core.min.js";

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
  const button = document.querySelector(`[popovertarget="${popover.id}"]`).getBoundingClientRect();
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
      .sort((a, b) => uses.get(b) - uses.get(a) || a.localeCompare(b));
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
  return html`<input
    type="search"
    placeholder="Filter properties and tags"
    aria-label="Filter properties and tags"
    .value=${view.filter}
    @input=${(event) => (view.filter = event.target.value)}
  />`;
}

/** Return the entries whose names have the text of the filter box of a view in them. */
export function filtered(view, entries) {
  const wanted = view.filter.trim().toLowerCase();
  return entries.filter((entry) => entry.name.toLowerCase().includes(wanted));
}
