#!/bin/bash

export PYTHONPATH=$(pwd)/anki/pylib/anki/_vendor
anki/out/pyenv/bin/python ./tools/genfluent/genfluent.py
