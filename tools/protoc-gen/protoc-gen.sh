#!/bin/bash

root=$(dirname $0)/../..
export PYTHONPATH=$root/anki/pylib/anki/_vendor
export PATH=$root/anki/out/extracted/protoc/bin:"$PATH"
$root/anki/out/pyenv/bin/python $root/tools/protoc-gen/protoc-gen.py
