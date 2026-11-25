// The first path segment of the REST API. It has to match `channelfinder.legacy.service-root`.
//
// A site that changes that property serves its own copy of this file. Put a `config.js` with the
// other value in a directory, and list that directory before the built-in files:
//
//   spring.web.resources.static-locations=file:/etc/channelfinder/web/,classpath:/static/,classpath:/META-INF/resources/
//
// The last entry is where Lit is served from.
export const SERVICE_ROOT = "ChannelFinder";
