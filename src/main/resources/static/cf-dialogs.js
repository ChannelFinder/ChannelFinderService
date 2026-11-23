import { LitElement, html, nothing } from "./vendor/lit-core.min.js";
import { basicLogin, send, setCredentials } from "./api.js";
import { EDITS, createChannels, createDefinition, editSelection, nothingToEdit } from "./writes.js";
import { byName, emit, formatNumber, plural } from "./shared.js";

/** @typedef {import("./writes.js").WriteSpec} WriteSpec */

// A dialog names at most this many of the channels that a change touches.
const NAMES_SHOWN = 10;
const NOT_SET = html`<i>not set</i>`;
const OWNER_LABEL = "Owner: user or group allowed to change it";
// Safari does not know the `closedby` attribute yet.
const HAS_CLOSEDBY = "closedBy" in HTMLDialogElement.prototype;

/** Say what went wrong, and how far a change of several requests got. */
function describeFailure(error, done, requests) {
  const total = requests.length;
  const { channel } = requests[done];
  let text = error.message;
  if (channel) text += ` This was for the channel ${channel}.`;
  if (total > 1) text += ` Request ${done + 1} of ${total} failed; ${done} were done before it.`;
  if (!done && (error.status === 401 || error.status === 403)) text += " Nothing was changed.";
  return text;
}

/**
 * The modal dialogs. `spec.type` says which one is open: `compare` for the selected channels side
 * by side, `edit` to choose a tag or a property for the selection, `create` for a new tag, property
 * or channels, and `write` to confirm a change.
 */
class CfDialogs extends LitElement {
  static properties = {
    spec: { attribute: false },
    user: { attribute: false },
    selection: { attribute: false },
    properties: { attribute: false },
    tags: { attribute: false },
    onlyDiffering: { state: true },
    editName: { state: true },
    sending: { state: true },
    partlyDone: { state: true },
    // Set when the server refused the login that was kept, so that another one can be given.
    loginRefused: { state: true },
    // How many channels are listed in the form that creates channels.
    listed: { state: true },
    error: { state: true },
  };

  createRenderRoot() {
    return this;
  }

  get dialog() {
    return this.querySelector("dialog");
  }

  /** The tags or the properties, whichever the open edit is about. */
  get entries() {
    return EDITS[this.spec.kind].kind === "tag" ? this.tags : this.properties;
  }

  willUpdate(changed) {
    if (!changed.has("spec")) return;
    this.onlyDiffering = false;
    this.sending = false;
    this.partlyDone = false;
    this.loginRefused = false;
    this.listed = 0;
    this.error = "";
    if (this.spec?.type === "edit") this.editName = this.entries[0]?.name ?? "";
  }

  updated(changed) {
    if (!changed.has("spec")) return;
    if (!this.spec) {
      this.dialog.close();
      return;
    }
    // Where the content was swapped while the dialog stayed open, the focus is moved by hand.
    if (this.dialog.open) this.dialog.querySelector("[autofocus]")?.focus();
    else this.dialog.showModal();
  }

  close() {
    emit(this, "cf-dialog", null);
  }

  /** Tell the root that the user closed the dialog, for example with Esc. */
  onClose() {
    // The event of an earlier close can arrive after the next dialog has opened.
    if (this.spec && !this.dialog.open) this.close();
  }

  /** Esc must not close the dialog while a change is being sent. */
  onCancel(event) {
    if (this.sending) event.preventDefault();
  }

  /** Close on a click on the backdrop, in a browser that does not do so itself. */
  onClick(event) {
    if (HAS_CLOSEDBY || this.sending || event.target !== this.dialog) return;
    // A click on the backdrop has the dialog as its target, and so has one on its padding.
    const box = this.dialog.getBoundingClientRect();
    const inside =
      event.clientX >= box.left &&
      event.clientX <= box.right &&
      event.clientY >= box.top &&
      event.clientY <= box.bottom;
    if (!inside) this.dialog.close();
  }

  /**
   * Build a change from a form and send it. A removal, or a change with something to point
   * out, goes to the dialog that confirms it first. An error is shown in the form.
   *
   * @param {(form: FormData) => Promise<WriteSpec>} build
   */
  async submitForm(event, build) {
    event.preventDefault();
    const form = new FormData(event.target);
    this.error = "";
    let spec;
    try {
      spec = await build(form);
    } catch (error) {
      this.error = error.message;
      return;
    }
    if (spec.danger) emit(this, "cf-dialog", { type: "write", ...spec });
    else await this.send(spec, form);
  }

  async submitEdit(event) {
    const definition = this.entries.find((entry) => entry.name === this.editName);
    await this.submitForm(event, (form) =>
      editSelection(this.spec.kind, definition, form.get("value"), this.selection),
    );
  }

  async submitCreate(event) {
    const { kind } = this.spec;
    await this.submitForm(event, (form) => {
      const owner = form.get("owner").trim();
      if (kind === "channels") return createChannels(form.get("channels"), owner);
      return createDefinition(kind, form.get("name").trim(), owner);
    });
  }

  async submitWrite(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    const { typeToConfirm } = this.spec;
    if (typeToConfirm && form.get("typed").trim() !== typeToConfirm) {
      this.error = `Type ${typeToConfirm} to confirm.`;
      return;
    }
    await this.send(this.spec, form);
  }

  /**
   * Send the requests one after another, and stop at the first that fails.
   *
   * @param {WriteSpec} spec
   * @param {FormData} form The form of the open dialog, which has the login fields where needed.
   */
  async send(spec, form) {
    // A login from the form is tried first, and kept only once a change has gone through with it.
    const user = form.get("user")?.trim() ?? this.user;
    const login = form.has("user") ? basicLogin(user, form.get("password")) : undefined;
    this.sending = true;
    this.error = "";
    let done = 0;
    let failure = null;
    try {
      for (const { method, path, body } of spec.requests) {
        await send(method, path, body, login); // NOSONAR: one at a time, to stop at the first that fails
        done += 1;
      }
    } catch (error) {
      failure = error;
    }
    if (login && done) setCredentials(login);
    this.sending = false;
    if (failure) {
      this.loginRefused = failure.status === 401;
      this.partlyDone = done > 0;
      this.error = describeFailure(failure, done, spec.requests);
      if (done) emit(this, "cf-reload");
      return;
    }
    emit(this, "cf-written", { user, message: spec.done, clearSelection: spec.clearSelection });
  }

  render() {
    const type = this.spec?.type;
    return html`<dialog
      id="cf-dialog"
      class=${type === "compare" ? "cf-wide" : ""}
      aria-labelledby="cf-dialog-title"
      closedby=${this.sending ? "none" : "any"}
      @close=${this.onClose}
      @cancel=${this.onCancel}
      @click=${this.onClick}
    >
      ${type === "compare" ? this.renderCompare() : nothing} ${type === "edit" ? this.renderEdit() : nothing}
      ${type === "create" ? this.renderCreate() : nothing} ${type === "write" ? this.renderWrite() : nothing}
    </dialog>`;
  }

  renderCompare() {
    const channels = [...this.selection.values()];
    const names = new Set(channels.flatMap((channel) => channel.properties.map((property) => property.name)));
    const valueOf = (channel, name) => channel.properties.find((property) => property.name === name)?.value;
    const tagsOf = (channel) => channel.tags.toSorted(byName).map((tag) => tag.name);
    const rows = [...names].map((name) => ({ name, values: channels.map((c) => valueOf(c, name)) }));
    rows.sort(byName);
    rows.push(
      { name: "tags", values: channels.map((channel) => tagsOf(channel).join(" ")) },
      { name: "owner", values: channels.map((channel) => channel.owner) },
    );
    for (const row of rows) row.differs = new Set(row.values).size > 1;
    const differing = rows.filter((row) => row.differs).length;
    const shown = this.onlyDiffering ? rows.filter((row) => row.differs) : rows;
    return html`<div class="cf-dialog-body">
      <h2 id="cf-dialog-title">Compare ${formatNumber(channels.length)} channels</h2>
      <p>
        <label>
          <input
            type="checkbox"
            .checked=${this.onlyDiffering}
            @change=${(event) => (this.onlyDiffering = event.target.checked)}
          />
          Only rows that differ
        </label>
        <span class="cf-small cf-differing">${plural(differing, "row differs", "rows differ")}</span>
      </p>
      <div class="cf-compare" tabindex="0" role="region" aria-label="Comparison">
        <table class="cf-table">
          <thead>
            <tr>
              <th></th>
              ${channels.map((channel) => html`<th scope="col" class="cf-mono">${channel.name}</th>`)}
            </tr>
          </thead>
          <tbody>
            ${shown.map(
              (row) =>
                html`<tr class=${row.differs ? "cf-differs" : ""}>
                  <th scope="row">${row.name}</th>
                  ${row.values.map((value) => html`<td>${value ?? NOT_SET}</td>`)}
                </tr>`,
            )}
          </tbody>
        </table>
      </div>
      <div class="cf-foot">
        <button class="cf-btn" type="button" autofocus @click=${this.close}>Close</button>
      </div>
    </div>`;
  }

  renderEdit() {
    const { title, kind, removes } = EDITS[this.spec.kind];
    const selected = this.selection.size;
    let problem = nothingToEdit(this.spec.kind, this.editName, this.selection);
    if (!this.entries.length) problem = `There is no ${kind} yet. Create one under Manage.`;
    // An addition is sent from this form. A removal is confirmed in the next dialog.
    const reach = this.spec.kind === "add-tag" ? "to" : "on";
    const action = removes ? title : `${title} ${reach} ${plural(selected, "channel", "channels")}`;
    return html`<form class="cf-dialog-body cf-stack" @submit=${this.submitEdit}>
      <h2 id="cf-dialog-title">${title}</h2>
      <label>
        ${kind === "tag" ? "Tag" : "Property"}
        <select autofocus @change=${(event) => (this.editName = event.target.value)}>
          ${this.entries.map(
            ({ name }) => html`<option ?selected=${name === this.editName}>${name}</option>`,
          )}
        </select>
      </label>
      ${this.spec.kind === "set-property"
        ? html`<label>Value <input type="text" name="value" required /></label>`
        : nothing}
      <p class="cf-small">${problem}</p>
      ${removes ? nothing : this.renderLogin()}
      <p class="cf-error" role="alert">${this.error}</p>
      <div class="cf-foot">
        <button class="cf-btn" type="button" ?disabled=${this.sending} @click=${this.close}>Cancel</button>
        <button class="cf-btn cf-primary" ?disabled=${this.sending || !!problem}>
          ${this.sending ? "Sending…" : action}
        </button>
      </div>
    </form>`;
  }

  renderCreate() {
    const { kind } = this.spec;
    const forChannels = kind === "channels";
    const title = forChannels ? "Create channels" : `New ${kind}`;
    let action = `Create ${kind}`;
    if (forChannels) action = this.listed ? `Create ${plural(this.listed, "channel", "channels")}` : title;
    const countLines = (event) =>
      (this.listed = event.target.value.split("\n").filter((line) => line.trim()).length);
    return html`<form class="cf-dialog-body cf-stack" @submit=${this.submitCreate}>
      <h2 id="cf-dialog-title">${title}</h2>
      ${forChannels
        ? html`<p class="cf-small">
              Existing channels keep what they have; these tags and properties are added.
            </p>
            <label>
              One channel per line
              <textarea
                name="channels"
                rows="6"
                required
                autofocus
                placeholder="name  tag  property=value"
                @input=${countLines}
              ></textarea>
            </label>`
        : html`<label>Name <input type="text" name="name" required autofocus /></label>`}
      <label>${OWNER_LABEL} <input type="text" name="owner" required .value=${this.user ?? ""} /></label>
      ${this.renderLogin()}
      <p class="cf-error" role="alert">${this.error}</p>
      <div class="cf-foot">
        <button class="cf-btn" type="button" ?disabled=${this.sending} @click=${this.close}>Cancel</button>
        <button class="cf-btn cf-primary" ?disabled=${this.sending}>
          ${this.sending ? "Sending…" : action}
        </button>
      </div>
    </form>`;
  }

  /** The login fields, where no login is kept or the server refused the one that is. */
  renderLogin() {
    if (this.user && !this.loginRefused) return html``;
    return html`<fieldset class="cf-login">
      <legend class="cf-small">
        <b>Log in to make this change.</b> Kept in this tab until you click Forget.
      </legend>
      <input
        type="text"
        id="cf-login-user"
        name="user"
        required
        placeholder="User name"
        aria-label="User name"
        autocomplete="username"
        .value=${this.user ?? ""}
      />
      <input
        type="password"
        id="cf-login-password"
        name="password"
        required
        placeholder="Password"
        aria-label="Password"
        autocomplete="current-password"
      />
    </fieldset>`;
  }

  renderWrite() {
    /** @type {WriteSpec} */
    const spec = this.spec;
    const { names = [] } = spec;
    const more = names.length - NAMES_SHOWN;
    return html`<form class="cf-dialog-body" @submit=${this.submitWrite}>
      <h2 id="cf-dialog-title">${spec.title}</h2>
      <p>${spec.body}</p>
      <ul class="cf-names cf-mono">
        ${names.slice(0, NAMES_SHOWN).map((name) => html`<li>${name}</li>`)}
        ${more > 0 ? html`<li>and ${formatNumber(more)} more</li>` : nothing}
      </ul>
      ${spec.typeToConfirm
        ? html`<label class="cf-typed">
            Type <b>${spec.typeToConfirm}</b> to confirm.
            <input type="text" name="typed" required autocomplete="off" spellcheck="false" />
          </label>`
        : nothing}
      ${this.renderLogin()}
      <p class="cf-error" id="cf-write-failure" role="alert">${this.error}</p>
      <div class="cf-foot">
        <button class="cf-btn" type="button" autofocus ?disabled=${this.sending} @click=${this.close}>
          ${this.partlyDone ? "Close" : "Cancel"}
        </button>
        <button
          class="cf-btn ${spec.danger ? "cf-danger" : "cf-primary"}"
          id="cf-write-send"
          ?disabled=${this.sending || this.partlyDone}
        >
          ${this.sending ? "Sending…" : (spec.action ?? spec.title)}
        </button>
      </div>
    </form>`;
  }
}

customElements.define("cf-dialogs", CfDialogs);
