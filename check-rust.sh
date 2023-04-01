#!/bin/bash

set -e

rustup component add clippy && cargo clippy
(cd anki/cargo/format && cargo fmt --check --all --manifest-path ../../../Cargo.toml)