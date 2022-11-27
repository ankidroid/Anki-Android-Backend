#!/bin/bash
#
# The buildinfo.txt file should be generated as part of the build,
# but for now we'll just check it into source control.

if ! bazel --version > /dev/null 2>&1; then
  echo "bazel: command not found. Please install Bazelisk. Your distro may have it,"
  echo "or you can fetch the binary from https://github.com/bazelbuild/bazelisk/releases"
  echo "and rename it to /usr/local/bin/bazel"
  exit 1
fi

(cd anki && bazel build -c opt buildinfo.txt)
cp anki/.bazel/bin/buildinfo.txt rslib-bridge