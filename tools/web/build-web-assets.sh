#!/bin/bash
# build web assets with anki submodule

set -e

SRC_DIR=$(pwd)
ANKI_SRC=$SRC_DIR/anki
BUILD_DIR=$SRC_DIR/rsdroid/build/generated/anki_artifacts
mkdir -p $BUILD_DIR/web

# build reviewer_extras_bundle and pages files
cd $ANKI_SRC

./ninja extract:protoc ts:reviewer:reviewer_extras_bundle.js qt/aqt:data/web/pages
cp out/qt/_aqt/data/web/pages/* out/ts/reviewer/reviewer_extras_bundle.js $BUILD_DIR/web/

cp $ANKI_SRC/cargo/licenses.json $BUILD_DIR/web/licenses-cargo.json
cp $ANKI_SRC/ts/licenses.json $BUILD_DIR/web/licenses-ts.json

chmod -R a+w $BUILD_DIR
