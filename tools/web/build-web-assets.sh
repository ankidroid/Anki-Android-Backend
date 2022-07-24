#!/bin/bash
# build web assets with anki submodule in rslib-bridge

set -e

# check if bazel exist or not
if ! bazel --version > /dev/null 2>&1; then
  echo "bazel: command not found. Please install Bazelisk. Your distro may have it,"
  echo "or you can fetch the binary from https://github.com/bazelbuild/bazelisk/releases"
  echo "and rename it to /usr/local/bin/bazel"
  exit 1
fi

SRC_DIR=$(pwd)
ANKI_SRC=$SRC_DIR/rslib-bridge/anki
BUILD_DIR=$SRC_DIR/rsdroid/build/generated/anki_artifacts
mkdir -p $BUILD_DIR/web

# build buildinfo.txt, reviewer_extras_bundle and pages files
cd $ANKI_SRC

bazel build --symlink_prefix=/tmp/.bazel/ -c opt buildinfo.txt
cp /tmp/.bazel/bin/buildinfo.txt $BUILD_DIR/buildinfo.txt

bazel build ts/reviewer/reviewer_extras_bundle --symlink_prefix=/tmp/.bazel/
cp /tmp/.bazel/bin/ts/reviewer/reviewer_extras_bundle.js $BUILD_DIR/web

bazel build qt/aqt/data/web/pages --symlink_prefix=/tmp/.bazel/
cp -r /tmp/.bazel/bin/qt/aqt/data/web/pages/* $BUILD_DIR/web

cp $ANKI_SRC/cargo/licenses.json $BUILD_DIR/web/licenses-cargo.json
cp $ANKI_SRC/ts/licenses.json $BUILD_DIR/web/licenses-ts.json

chmod -R a+w $BUILD_DIR
