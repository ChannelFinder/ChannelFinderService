# Vendored files

The web UI loads nothing from the network, so the one library it uses is kept here.

## `lit-core.min.js`

Lit 3.3.3, the bundled build with `lit-html`, `lit-element` and `@lit/reactive-element` in one file.

- Source: the `lit/dist` repository on GitHub, tag `v3.3.3`, file `core/lit-core.min.js`. The `lit` package on
  npm only has the unbundled modules.
- SHA-256: `f607f470475d8ab790754cb70f72cdbe2390c4a57ab59df6cdb360eeb72bd87c`
- Licence: BSD-3-Clause. `lit-LICENSE` is the `LICENSE` file of the `lit` 3.3.3 package on npm.

The file is unchanged. Its last line names a source map that is not shipped, which only matters in the
browser's developer tools.

## Updating

1. Download `core/lit-core.min.js` from the new tag of `lit/dist`.
2. Check it against the same file on a second source, for example the tag as served by jsDelivr.
3. Replace the file, and `lit-LICENSE` if the licence text of the `lit` package has changed.
4. Update the version, tag and SHA-256 above (`shasum -a 256 lit-core.min.js`).
5. Run `WebUiIT`.
