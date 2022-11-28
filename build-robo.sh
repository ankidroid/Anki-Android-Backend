#!/bin/bash
#
# Builds the .jar file containing the backend library compiled for 
# desktop, which Robolectric needs. By default only the library for
# the current architecture is built (eg X86_64, or Arm64 on Apple M1+).
#
# Define ALL_ARCHS=1 to build a multi-platform bundle. This will only
# work on macOS, as it's not possible to target macOS from other platforms.
#
# Define DEBUG=1 to compile the Rust in debug mode, which builds faster
# but runs slower.
#

set -e

if [ "$DEBUG" = "1" ]; then
    release=""
    dir="debug"
else
    release="--release"
    dir="release"
fi

# Build the shared libraries
if [ "$ALL_ARCHS" = "1" ]; then
    # Mac
    for target in \
        x86_64-apple-darwin \
        aarch64-apple-darwin
    do
        rustup target add $target
        cargo build -p rsdroid $release --target $target
    done
    lipo -create target/x86_64-apple-darwin/$dir/librsdroid.dylib \
        target/aarch64-apple-darwin/$dir/librsdroid.dylib \
        -output rsdroid-testing/assets/librsdroid.dylib

    # Linux
    target=x86_64-unknown-linux-gnu
    rustup target add $target
    CC=x86_64-unknown-linux-gnu-gcc \
        CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER=x86_64-unknown-linux-gnu-gcc \
        cargo build -p rsdroid $release --target $target
    cp -v target/$target/$dir/librsdroid.so rsdroid-testing/assets


    # Windows
    target=x86_64-pc-windows-gnu
    rustup target add $target
    cargo build -p rsdroid $release --target $target
    cp -v target/$target/$dir/rsdroid.dll rsdroid-testing/assets
else
    cargo build -p rsdroid $release
    for file in target/$dir/librsdroid.{dylib,so}; do
        test -f $file && cp -v $file rsdroid-testing/assets/
    done
fi

# Bundle them up into a jar
./gradlew rsdroid-testing:build
