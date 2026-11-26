import { LitElement, html, nothing } from "lit";
import { deleteDefinition } from "../api/writes.js";
import { ICONS, emit, filtered, plural, renderListsState, renderNameFilter } from "../shared.js";

// The two kinds of definition: what one is called, and what several are called.
const NOUNS = { property: ["property", "properties"], tag: ["tag", "tags"] };

/**
 * The manage view: the properties or the tags that exist, with buttons that open the dialogs to
 * create them and channels.
 */
class CfManage extends LitElement {
  static properties = {
    properties: { attribute: false },
    tags: { attribute: false },
    listError: { attribute: false },
    notice: { attribute: false },
    // Which list is shown: `property` or `tag`.
    kind: { state: true },
    filter: { state: true },
    deleteError: { state: true },
  };

  constructor() {
    super();
    this.kind = "property";
    this.filter = "";
    this.deleteError = "";
  }

  createRenderRoot() {
    return this;
  }

  open(dialog) {
    this.deleteError = "";
    emit(this, "cf-dialog", dialog);
  }

  /** Deleting needs no form, so it goes straight to the dialog that confirms it. */
  async confirmDelete(definition) {
    try {
      this.open({ type: "write", ...(await deleteDefinition(definition)) });
    } catch (error) {
      this.deleteError = error.message;
    }
  }

  /** With two tabs, either arrow key goes to the other one. */
  async onTabKey(event) {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
    this.kind = this.kind === "property" ? "tag" : "property";
    await this.updateComplete;
    this.querySelector('[aria-selected="true"]').focus();
  }

  render() {
    return html`<section class="cf-pane cf-manage">
      <header>
        <div>
          <h2>Manage</h2>
          <p class="cf-small">Properties and tags that can be assigned to channels.</p>
        </div>
        <button
          class="cf-btn cf-primary"
          type="button"
          @click=${() => this.open({ type: "create", kind: "channels" })}
        >
          ${ICONS.plus} Create channels
        </button>
      </header>
      <div class="cf-tabbar">
        <div class="cf-views" role="tablist" aria-label="Kind of definition" @keydown=${this.onTabKey}>
          ${this.renderTab("property", "Properties")} ${this.renderTab("tag", "Tags")}
        </div>
        ${renderNameFilter(this)}
        <button class="cf-btn" type="button" @click=${() => this.open({ type: "create", kind: this.kind })}>
          ${ICONS.plus} New ${this.kind}
        </button>
      </div>
      <p class="cf-small" role="status">${this.notice}</p>
      <p class="cf-error" role="alert">${this.deleteError}</p>
      ${renderListsState(this) ?? this.renderList()}
    </section>`;
  }

  renderTab(kind, label) {
    const selected = this.kind === kind;
    return html`<button
      class="cf-plain"
      type="button"
      role="tab"
      id="cf-tab-${kind}"
      aria-controls="cf-definitions"
      aria-selected=${selected}
      tabindex=${selected ? 0 : -1}
      @click=${() => (this.kind = kind)}
    >
      ${label}
    </button>`;
  }

  /** The definitions of the chosen kind, in the order of the list in the search view. */
  renderList() {
    const [one, many] = NOUNS[this.kind];
    const shown = filtered(this, this.kind === "tag" ? this.tags : this.properties);
    const wanted = this.filter.trim();
    let foot = plural(shown.length, one, many);
    if (wanted) foot = `${plural(shown.length, one + " matches", many + " match")} “${wanted}”`;
    return html`<div
      class="cf-results"
      id="cf-definitions"
      role="tabpanel"
      aria-labelledby="cf-tab-${this.kind}"
    >
      ${shown.length ? this.renderTable(shown) : nothing}
      <p class="cf-small">${foot}</p>
    </div>`;
  }

  renderTable(entries) {
    const { kind } = this;
    return html`<table class="cf-table cf-grid cf-definitions">
      <thead>
        <tr>
          <th>Name</th>
          <th>Owner</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        ${entries.map(
          ({ name, owner }) =>
            html`<tr>
              <td title=${name}>${name}</td>
              <td title=${owner}>${owner}</td>
              <td>
                <button
                  class="cf-plain cf-icon-btn"
                  type="button"
                  aria-label="Delete ${kind} ${name}"
                  title="Delete"
                  @click=${() => this.confirmDelete({ kind, name, owner })}
                >
                  ${ICONS.trash}
                </button>
              </td>
            </tr>`,
        )}
      </tbody>
    </table>`;
  }
}

customElements.define("cf-manage", CfManage);
