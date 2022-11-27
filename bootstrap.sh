#!/bin/bash

set -e

git submodule update --init
(cd rslib-bridge/anki && ./ninja extract:protoc ftl:repo)
