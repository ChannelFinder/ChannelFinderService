/**
 * The changes that the page can make.
 *
 * Each function returns a spec: the requests of one change and the words that describe it. The
 * dialogs show the spec and send the requests.
 */
import { PATHS, get, getChannels } from "./api.js";
import { parseChannelLines } from "./query.js";
import { plural } from "../shared.js";

/**
 * @typedef {import("./query.js").Channel} Channel
 * @typedef {import("./query.js").Named} Named
 * @typedef {object} WriteRequest
 * @property {"PUT" | "POST" | "DELETE"} method
 * @property {string[]} path
 * @property {unknown} [body]
 * @property {string} [channel] The one channel that the request changes, to name it if it fails.
 * @typedef {object} WriteSpec
 * @property {string} title The action and what it is done to. Also the label of the button.
 * @property {string} [action] The label of the button, where the title has a name or a number in it.
 * @property {string} [body] What happens, in a sentence or two.
 * @property {string[]} [names] The channels that change, where a list helps.
 * @property {string} done The message shown after the change was made.
 * @property {boolean} [danger] Set for deletes and removals, which are confirmed before they are sent.
 * @property {string} [typeToConfirm] Text the user has to type before the change is sent.
 * @property {boolean} [clearSelection]
 * @property {WriteRequest[]} requests
 */

// A delete that touches more channels than this has to be confirmed by typing.
const TYPE_TO_CONFIRM_FROM = 100;
// The server answers 400 to a request path with an encoded `/` or `%` in it.
const NOT_IN_PATHS = /[/%]/;
const NOT_UNDONE = "This can't be undone.";

/** The edits of the selected channels that name a tag or a property. */
export const EDITS = {
  "add-tag": { title: "Add tag", kind: "tag", removes: false },
  "remove-tag": { title: "Remove tag", kind: "tag", removes: true },
  "set-property": { title: "Set property", kind: "property", removes: false },
  "remove-property": { title: "Remove property", kind: "property", removes: true },
};

const definitionPath = (kind, name) => [...(kind === "tag" ? PATHS.tags : PATHS.properties), name];
const capitalised = (kind) => (kind === "tag" ? "Tag" : "Property");
const channels = (n) => plural(n, "channel", "channels");
const namesOf = (list) => list.map((channel) => channel.name);

/** Refuse names that the server does not take in a request path. */
function requirePathNames(names) {
  if (names.some((name) => NOT_IN_PATHS.test(name))) {
    throw new Error("Names with / or % can't be changed from this page; use the REST API.");
  }
}

/** Return the selected channels that have the tag or the property. */
function having(selection, kind, name) {
  const entries = (channel) => (kind === "tag" ? channel.tags : channel.properties);
  return [...selection.values()].filter((channel) => entries(channel).some((entry) => entry.name === name));
}

/**
 * Return why an edit of the selected channels would do nothing, or "" if it does something.
 *
 * @param {keyof typeof EDITS} edit
 * @param {Map<string, Channel>} selection
 */
export function nothingToEdit(edit, name, selection) {
  const { kind, removes } = EDITS[edit];
  const count = having(selection, kind, name).length;
  if (removes && !count) return `None of the selected channels has ${name}.`;
  if (edit === "add-tag" && count === selection.size) return `All ${count} already have it.`;
  return "";
}

/**
 * @param {Map<string, Channel>} selection
 * @returns {WriteSpec}
 */
export function deleteChannels(selection) {
  const names = [...selection.keys()];
  return {
    title: `Delete ${channels(names.length)}`,
    action: "Delete channels",
    body: NOT_UNDONE,
    names,
    done: `${channels(names.length)} deleted`,
    danger: true,
    clearSelection: true,
    typeToConfirm: names.length > TYPE_TO_CONFIRM_FROM ? String(names.length) : undefined,
    requests: [{ method: "DELETE", path: PATHS.channels, body: names }],
  };
}

/**
 * Add or remove a tag or a property on the selected channels.
 *
 * @param {keyof typeof EDITS} edit
 * @param {Named} definition The tag or property as it is defined, with its owner.
 * @param {string} value The value of the property, for `set-property`.
 * @param {Map<string, Channel>} selection
 * @returns {WriteSpec}
 */
export function editSelection(edit, definition, value, selection) {
  const { title, kind, removes } = EDITS[edit];
  const { name, owner } = definition;
  const problem = nothingToEdit(edit, name, selection);
  if (problem) throw new Error(problem);
  requirePathNames([name]);
  const path = definitionPath(kind, name);
  const names = [...selection.keys()];

  if (removes) {
    const losing = namesOf(having(selection, kind, name));
    requirePathNames(losing);
    return {
      title: `${title} ${name}`,
      action: title,
      body: `${name} will be removed from ${channels(losing.length)}.`,
      names: losing,
      done: `${name} removed from ${channels(losing.length)}`,
      danger: true,
      requests: losing.map((channel) => ({ method: "DELETE", path: [...path, channel], channel })),
    };
  }
  // The channels are sent without an owner, so that each keeps the owner it has. The tag or
  // property keeps its owner too.
  const properties = kind === "tag" ? undefined : [{ name, owner, value }];
  const body = { name, owner, channels: names.map((channel) => ({ name: channel, properties })) };
  return {
    title: `${title} ${name}`,
    done: kind === "tag" ? `Tag ${name} added` : `${name} set`,
    requests: [{ method: "POST", path, body }],
  };
}

/**
 * @param {Named & { kind: "tag" | "property" }} definition
 * @returns {Promise<WriteSpec>}
 */
export async function deleteDefinition({ kind, name }) {
  requirePathNames([name]);
  // One count request, so that the dialog can say how many channels lose it.
  const params = new URLSearchParams(kind === "tag" ? { "~tag": name } : { [name]: "*" });
  const count = Number(await get(PATHS.count, params));
  const loss = count ? `${name} will be removed from ${channels(count)}.` : `No channel has ${name}.`;
  return {
    title: `Delete ${kind} ${name}`,
    action: `Delete ${kind}`,
    body: `${loss} ${NOT_UNDONE}`,
    done: `${capitalised(kind)} ${name} deleted`,
    danger: true,
    typeToConfirm: count > TYPE_TO_CONFIRM_FROM ? name : undefined,
    requests: [{ method: "DELETE", path: definitionPath(kind, name) }],
  };
}

/**
 * @param {"tag" | "property"} kind
 * @returns {Promise<WriteSpec>}
 */
export async function createDefinition(kind, name, owner) {
  requirePathNames([name]);
  // Creating a name that exists replaces it, which removes it from every channel. The list on
  // the page may be out of date, so it is read again. Case is ignored, because the server does
  // not keep names apart that differ only in case: deleting one also removes the other from
  // its channels.
  const existing = await get(kind === "tag" ? PATHS.tags : PATHS.properties);
  const taken = existing.find((entry) => entry.name.toLowerCase() === name.toLowerCase());
  if (taken) throw new Error(`A ${kind} called ${taken.name} already exists (owner ${taken.owner}).`);
  return {
    title: `Create ${kind} ${name}`,
    done: `${capitalised(kind)} ${name} created`,
    requests: [{ method: "PUT", path: definitionPath(kind, name), body: { name, owner } }],
  };
}

/**
 * Create channels, or add tags and properties to channels that exist.
 *
 * @param {string} text One channel per line, as `name tag property=value`.
 * @param {string} owner The owner of the channels that are new.
 * @returns {Promise<WriteSpec>}
 */
export async function createChannels(text, owner) {
  const lines = parseChannelLines(text);
  if (!lines.length) throw new Error("List at least one channel.");
  // POST merges into a channel that exists. Such a channel is sent with the owner it has, so
  // that the merge does not change it.
  const existing = new Map((await getChannels(namesOf(lines))).map((channel) => [channel.name, channel]));
  const body = lines.map((line) => ({ ...line, owner: existing.get(line.name)?.owner ?? owner }));
  return {
    title: "Create channels",
    done: `${channels(lines.length)} written`,
    requests: [{ method: "POST", path: PATHS.channels, body }],
  };
}
