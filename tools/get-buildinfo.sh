#!/bin/bash
#
# The buildinfo.txt file should be generated as part of the build,
# but for now we'll just check it into source control.

(cd rslib-bridge/anki && bazel build -c opt buildinfo.txt)
cp rslib-bridge/anki/.bazel/bin/buildinfo.txt rslib-bridge