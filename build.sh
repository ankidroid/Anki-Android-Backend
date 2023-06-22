#!/bin/bash
#
# Builds:
# - desktop web components
# - Android JNI library and auto-generated Kotlin interface
# - Robolectric JNI library.
#
# The first two will be bundled into an .aar by gradle, and the last
# is bundled into a .jar by gradle. Gradle will be automatically invoked
# if this script is run from the command line.
#
# By default, only the library for the current architecture is built
# (eg X86_64, or Arm64 on Apple M1+).
#
# Define ALL_ARCHS=1 to build a multi-platform bundle, which takes about 4x
# longer to download and build.
#
# Define RELEASE=1 to compile the Rust in release mode, which builds slower
# but runs faster.
#

set -e

if [ "$RUNNING_FROM_BUILD_SCRIPT" = "1" ]; then
    # this script invoked gradle which invoked this script; nothing left to do
    exit 0;
fi

# Android code built into target/, so we don't clobber robolectric cache
export CARGO_TARGET_DIR=target
ARTIFACTS_DIR=rsdroid/build/generated/anki_artifacts
JNI_DIR=rsdroid/build/generated/jniLibs

echo "*** Building desktop web components"
mkdir -p $ARTIFACTS_DIR/web
(cd anki && ./ninja extract:protoc ts:reviewer:reviewer_extras_bundle.js qt:aqt:data:web:pages)
cp anki/out/qt/_aqt/data/web/pages/* anki/out/ts/reviewer/reviewer_extras_bundle.js $ARTIFACTS_DIR/web/
cp anki/cargo/licenses.json $ARTIFACTS_DIR/web/licenses-cargo.json
cp anki/ts/licenses.json $ARTIFACTS_DIR/web/licenses-ts.json
chmod -R a+w $ARTIFACTS_DIR

# determine android target archs
if [ "$ALL_ARCHS" = "1" ]; then
    rustup target add armv7-linux-androideabi   # arm
    rustup target add i686-linux-android        # x86
    rustup target add aarch64-linux-android     # arm64
    rustup target add x86_64-linux-android      # x86_64
    targets="-t armv7 -t i686 -t aarch64 -t x86_64"
else
    if [[ "$OSTYPE" == "darwin"* && $(arch) == "arm64" ]]; then
        rustup target add aarch64-linux-android
        targets="-t aarch64"
    else
        rustup target add x86_64-linux-android
        targets="-t x86_64"
    fi
fi

if [ "$RELEASE" = "1" ]; then
    release="--release"
    release_dir="release"
else
    release=""
    release_dir="debug"
fi

echo
echo "*** Building Android JNI library + backend interface"
rm -rf $JNI_DIR
cargo install cargo-ndk@3.2.0
cargo ndk -o $JNI_DIR $targets build -p rsdroid $release

echo
echo "*** Building Robolectric JNI library"
# Robolectric build cache can be shared with desktop, as it's the same arch
export CARGO_TARGET_DIR=anki/out/rust
if [ "$ALL_ARCHS" = "1" ]; then
    if [[ "$OSTYPE" != "darwin"* ]]; then
        echo "Must be on macOS to do a multi-arch build."
        exit 1
    fi

    # Mac
    for target in \
        x86_64-apple-darwin \
        aarch64-apple-darwin
    do
        rustup target add $target
        cargo build -p rsdroid $release --target $target
    done
    lipo -create target/x86_64-apple-darwin/$release_dir/librsdroid.dylib \
        target/aarch64-apple-darwin/$release_dir/librsdroid.dylib \
        -output rsdroid-testing/assets/librsdroid.dylib

    # Linux
    target=x86_64-unknown-linux-gnu
    rustup target add $target
    CC=x86_64-unknown-linux-gnu-gcc \
        CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER=x86_64-unknown-linux-gnu-gcc \
        cargo build -p rsdroid $release --target $target
    cp -v target/$target/$release_dir/librsdroid.so rsdroid-testing/assets


    # Windows
    target=x86_64-pc-windows-gnu
    rustup target add $target
    cargo build -p rsdroid $release --target $target
    cp -v target/$target/$release_dir/rsdroid.dll rsdroid-testing/assets
else
    # Just build for current architecture
    cargo build -p rsdroid $release
    for file in target/$release_dir/librsdroid.{dylib,so}; do
        test -f $file && cp -v $file rsdroid-testing/assets/
    done
fi

export RUNNING_FROM_BUILD_SCRIPT=1
if [ "$RUNNING_FROM_GRADLE" != "1" ]; then
    # run gradle if invoked from command line
   ./gradlew assembleRelease rsdroid-testing:build
fi
