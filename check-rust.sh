#!/bin/bash

set -e

rustup component add rustfmt clippy
cargo clippy
cargo fmt -- --check
