#!/bin/bash

set -e

if [ ! -d anki/cargo/format ]; then
    echo "error: 'anki/cargo/format' not found: the anki submodule is not checked out." >&2
    echo "This is required after a fresh clone or in a new git worktree. To fix, run:" >&2
    echo "    git submodule update --init --recursive" >&2
    exit 1
fi

(cd anki/cargo/format && cargo fmt --all --manifest-path ../../../Cargo.toml)
