import { LitElement, html } from "./vendor/lit-core.min.js";
import { deleteDefinition } from "./writes.js";
import { emit, filtered, renderListsState, renderNameFilter } from "./shared.js";

/**
 * The manage view: the properties and the tags that exist, with buttons that open the dialogs to
 * create them and channels.
 */
class CfManage extends LitElement {
  static properties = {
    properties: { attribute: false },
    tags: { attribute: false },
    listError: { attribute: false },
    notice: { attribute: false },
    filter: { state: true },
    deleteError: { state: true },
  };

  constructor() {
    super();
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

  render() {
    return html`<section class="cf-pane">
      <div class="cf-toolbar">
        ${renderNameFilter(this)}
        <span class="cf-grow">
          <span class="cf-small" role="status">${this.notice}</span>
          <span class="cf-error" role="alert">${this.deleteError}</span>
        </span>
        <button class="cf-btn" type="button" @click=${() => this.open({ type: "create", kind: "channels" })}>
          Create channels
        </button>
      </div>
      ${renderListsState(this) ??
      html`${this.renderSection("property", "Properties", this.properties)}
      ${this.renderSection("tag", "Tags", this.tags)}`}
    </section>`;
  }

  /** One kind of definition under its heading, in the order of the list in the search view. */
  renderSection(kind, title, entries) {
    const shown = filtered(this, entries);
    return html`<header class="cf-section-head">
        <h2>${title}</h2>
        <button class="cf-btn" type="button" @click=${() => this.open({ type: "create", kind })}>
          New ${kind}
        </button>
      </header>
      <div class="cf-results">
        ${shown.length ? this.renderTable(kind, shown) : html`<p>No matching ${title.toLowerCase()}</p>`}
      </div>`;
  }

  renderTable(kind, entries) {
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
                  class="cf-btn cf-compact"
                  type="button"
                  aria-label="Delete ${kind} ${name}"
                  @click=${() => this.confirmDelete({ kind, name, owner })}
                >
                  Delete
                </button>
              </td>
            </tr>`,
        )}
      </tbody>
    </table>`;
  }
}

customElements.define("cf-manage", CfManage);
