#!/bin/bash

set -e

# Non-zero exit on warnings
export RUSTFLAGS="-Dwarnings"

if [ ! -d anki/cargo/format ]; then
    echo "error: 'anki/cargo/format' not found: the anki submodule is not checked out." >&2
    echo "This is required after a fresh clone or in a new git worktree. To fix, run:" >&2
    echo "    git submodule update --init --recursive" >&2
    exit 1
fi

rustup component add clippy && cargo clippy
(cd anki/cargo/format && cargo fmt --check --all --manifest-path ../../../Cargo.toml)