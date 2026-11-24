/**
 * The query text of the search box, and what it means as request parameters.
 *
 * A query is a list of words separated by spaces. `key=value` and `key!=value` are filters. The
 * first word that is not a filter is the name pattern, and later ones are tags. A value with
 * spaces in it is put in double quotes.
 */

/**
 * @typedef {{ raw: string, key: string, negated: boolean, value: string }} Token
 * @typedef {{ name: string, owner?: string, value?: string }} Named
 * @typedef {{ name: string, owner?: string, properties: Named[], tags: Named[] }} Channel
 */

const WORD = /(?:[^\s"]+|"[^"]*")+/g;
const FILTER = /^([^=!"]+)(!?)=(.*)$/;
const NAME = "~name";
const TAG = "~tag";

const unquote = (text) => text.replace(/^"(.*)"$/, "$1");

/** @returns {Token[]} */
export function tokens(text) {
  let named = false;
  return (text.match(WORD) ?? []).map((raw) => {
    const filter = raw.match(FILTER);
    if (filter) {
      if (filter[1] === NAME) named = true;
      return { raw, key: filter[1], negated: filter[2] === "!", value: unquote(filter[3]) };
    }
    const key = named ? TAG : NAME;
    named = true;
    return { raw, key, negated: false, value: unquote(raw) };
  });
}

/**
 * Return the request parameters for a query.
 *
 * `~size`, `~from` and the other paging keys are left out, because the page sets them itself.
 */
export function toParams(text) {
  const params = new URLSearchParams();
  for (const token of tokens(text)) {
    if (token.key.startsWith("~") && token.key !== NAME && token.key !== TAG) continue;
    params.append(token.key + (token.negated ? "!" : ""), token.value);
  }
  if (!params.has(NAME)) params.set(NAME, "*");
  return params;
}

/**
 * Return a line about a word of the query that cannot match as it is written, or "".
 *
 * @param {Named[]} properties The properties that are defined.
 */
export function queryHint(text, properties) {
  // Search does not tell upper and lower case apart, so neither does this.
  const known = new Map(properties.map((property) => [property.name.toLowerCase(), property.name]));
  for (const { raw, key } of tokens(text)) {
    if (FILTER.test(raw)) {
      if (!key.startsWith("~") && !known.has(key.toLowerCase())) return `There is no property called ${key}.`;
      continue;
    }
    // `iocName:value` is a name pattern or a tag. With a property in front, `=` was probably meant.
    const [prefix, ...rest] = raw.split(":");
    const property = known.get(prefix.toLowerCase());
    if (rest.length && property) return `Use ${property}=${rest.join(":")}, not ${raw}.`;
  }
  return "";
}

/**
 * Read channels from text with one channel per line, written as `name tag property=value`.
 *
 * @returns {Channel[]}
 */
export function parseChannelLines(text) {
  const channels = [];
  for (const line of text.split("\n")) {
    const [name, ...rest] = line.match(WORD) ?? [];
    if (!name) continue;
    const channel = { name: unquote(name), tags: [], properties: [] };
    for (const word of rest) {
      const property = word.match(/^([^=]+)=(.*)$/);
      if (property) channel.properties.push({ name: property[1], value: unquote(property[2]) });
      else channel.tags.push({ name: unquote(word) });
    }
    channels.push(channel);
  }
  return channels;
}
