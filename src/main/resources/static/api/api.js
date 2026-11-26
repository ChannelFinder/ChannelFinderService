/** Client for the ChannelFinder REST API. */
import { SERVICE_ROOT } from "../config.js";

const READ_TIMEOUT_MS = 10_000;
const WRITE_TIMEOUT_MS = 60_000;
const NAMES_PER_REQUEST_CHARS = 3000;
// The default of `elasticsearch.query.size`, the most channels one search returns.
export const RESULT_WINDOW = 10_000;
// Sent to `window` when a request gets no answer, so that the page can check the service.
export const UNREACHABLE_EVENT = "cf-unreachable";
// The resources below the service root. A name is added as one more segment.
export const PATHS = {
  info: [],
  channels: ["resources", "channels"],
  combined: ["resources", "channels", "combined"],
  count: ["resources", "channels", "count"],
  tags: ["resources", "tags"],
  properties: ["resources", "properties"],
};
const STATUS_MESSAGES = {
  401: "The user name or password is wrong.",
  403: "This user is not allowed to make this change.",
};

let authorization = null;

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

/** Return a login as the value of an `Authorization` header. */
export function basicLogin(user, password) {
  const bytes = new TextEncoder().encode(`${user}:${password}`);
  return "Basic " + btoa(String.fromCodePoint(...bytes));
}

/**
 * Keep a login from `basicLogin` for later writes, or forget it when called without one.
 *
 * The password is held in this module only. It is never put in storage or in a URL.
 */
export function setCredentials(login = null) {
  authorization = login;
}

/**
 * Return the URL of a resource.
 *
 * Each segment is encoded on its own, so a name with `?` or `#` in it cannot change which
 * resource the request reaches.
 *
 * @param {string[]} path
 * @param {URLSearchParams} [params]
 */
export function resourceUrl(path, params) {
  const encoded = path.map((segment) => "/" + encodeURIComponent(segment)).join("");
  const url = new URL(SERVICE_ROOT + encoded, document.baseURI);
  // Written out by hand so that a space is `%20`. URLSearchParams would write `+`.
  url.search = [...(params ?? [])]
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join("&");
  return url;
}

async function request(method, path, { params, body, login = authorization } = {}) {
  const isWrite = method !== "GET";
  const timeout = isWrite ? WRITE_TIMEOUT_MS : READ_TIMEOUT_MS;
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (isWrite && login) headers.Authorization = login;

  let response;
  let text;
  try {
    response = await fetch(resourceUrl(path, params), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      // Without this the browser shows its own login prompt when the server answers 401.
      credentials: "omit",
      signal: AbortSignal.timeout(timeout),
    });
    // The time limit also covers reading the answer.
    text = await response.text();
  } catch (error) {
    globalThis.dispatchEvent(new Event(UNREACHABLE_EVENT));
    if (error.name === "TimeoutError") {
      const unsure = isWrite ? " The change may still have been made." : "";
      throw new ApiError(`ChannelFinder did not answer within ${timeout / 1000} seconds.${unsure}`);
    }
    throw new ApiError("ChannelFinder is not reachable.");
  }

  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }
  if (!response.ok) {
    // The reason is in `detail` for problem+json and in `message` for other error bodies.
    const reason = data?.detail || data?.message || STATUS_MESSAGES[response.status] || "The request failed.";
    throw new ApiError(`${reason} (${response.status})`, response.status);
  }
  return data;
}

/**
 * @param {string[]} path
 * @param {URLSearchParams} [params]
 */
export const get = (path, params) => request("GET", path, { params });

/**
 * Send a write, with the login given here or else the one given to `setCredentials`.
 *
 * @param {"PUT" | "POST" | "DELETE"} method
 * @param {string[]} path
 * @param {unknown} [body]
 * @param {string} [login]
 */
export const send = (method, path, body, login) => request(method, path, { body, login });

/**
 * Return the channels with exactly these names. Names that do not exist are left out.
 *
 * The names are sent in batches, so that the request line stays within what servers accept.
 *
 * @param {string[]} names
 */
export async function getChannels(names) {
  const batches = [[]];
  let length = 0;
  for (const name of names) {
    if (length + name.length > NAMES_PER_REQUEST_CHARS) {
      batches.push([]);
      length = 0;
    }
    batches.at(-1).push(name);
    length += name.length + 1;
  }
  const answers = await Promise.all(
    batches
      .filter((batch) => batch.length)
      .map((batch) => {
        const params = new URLSearchParams({ "~name": batch.join("|"), "~size": String(RESULT_WINDOW) });
        return get(PATHS.channels, params);
      }),
  );
  // A name with a wildcard in it also matches other channels.
  const wanted = new Set(names);
  return answers.flat().filter((channel) => wanted.has(channel.name));
}
