# AnkiDroid-Backend

An interface for accessing Anki Desktop's Rust backend inside AnkiDroid.

## Why?

- Removes the need to port Anki Desktop's business logic to Java
  - 100% compatibility and no bugs
  - Rust should provide a speed increase
  - An upgrade for AnkiDroid should only require moving to a later commit in a submodule
  - Saves massive amount of AnkiDroid developer time & effort
  - Allows Anki Desktop to iterate faster
  - We can quickly port changes upstream, which will benefit the ecosystem

- In a separate repo, so most AnkiDroid developers do not need to install Rust
and build the backend.

## How to use

AnkiDroid uses a pre-built version of this library, and includes it in AnkiDroid/build.gradle.
To build a local version of this library and tell AnkiDroid to use it, please see
the [howto guide](./docs/HOWTO.md).

## Architecture

See [the overview](./docs/OVERVIEW.md).

## Additional Information

See `/docs` for more in-depth information.

## License

[GPL-3.0 License](https://github.com/ankidroid/Anki-Android/blob/master/COPYING)  
[AGPL-3.0 Licence](https://github.com/AnkiDroid/anki/blob/main/LICENSE) (anki submodule)
