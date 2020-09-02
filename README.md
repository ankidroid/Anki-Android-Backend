# AnkiDroid-Backend

Allows AnkiDroid to use `anki/rslib`

## Folders

`/anki/` - git submodule containing the Anki Codebase, used both for building into `.so` files, and to obtain the current `.proto` files for use in Java codegen

`/tools/` See `readme.md` - one active tool to generate Java service bindings

`/rsdroid/` - Java code to be consumed by `Anki-Android`.

`cargo.toml` takes a dependency on a given commit via  `/anki/`

`/rslib-bridge/` Android-specific Rust bindings to communicate with `anki/rslib`
