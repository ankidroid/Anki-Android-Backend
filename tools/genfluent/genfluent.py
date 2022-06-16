#!/usr/bin/env python3
# Copyright: Ankitects Pty Ltd and contributors
# License: GNU AGPL, version 3 or later; http://www.gnu.org/licenses/agpl.html

from __future__ import annotations

import json
from pathlib import Path
import subprocess
from typing import List, Literal, TypedDict
import stringcase
import re

def ensure_i18n_module_correct():
    reg = re.compile(r'(\s+)(\S+_(?:commit|zip_csum)) = "(.*)"')
    for line in open("rslib-bridge/anki/repos.bzl").readlines():
        if m := reg.match(line):
            (indent, key, commit) = m.groups()
            if key == "core_i18n_commit":
                subprocess.run(["git", "checkout", commit], cwd="ftl/core", check=True)
                break

def get_strings():
    output_file = Path("output.json").absolute()
    subprocess.run(["cargo", "run", output_file], check=True, cwd="rslib-bridge/anki/rslib/i18n")
    data = json.load(open(output_file))
    output_file.unlink()
    return data

modules = get_strings()

def build_source():
    out = """\
// Copyright: Ankitects Pty Ltd and contributors
// License: GNU AGPL, version 3 or later; http://www.gnu.org/licenses/agpl.html

package anki.i18n;

import anki.i18n.TranslateArgValue

fun asTranslateArg(arg: Any): TranslateArgValue {
    val builder = TranslateArgValue.newBuilder()
    when (arg) {
        is String -> builder.setStr(arg)
        is Int -> builder.setNumber(arg.toDouble())
        is Double -> builder.setNumber(arg)
        else -> throw Exception("invalid arg provided to translation")
    }
    return builder.build()
}

// This should be either String, Double or Int
typealias TranslateArg = Any

typealias TranslateArgMap = Map<String, TranslateArgValue>

interface GeneratedTranslations {
    fun translate(module: Int, translation: Int, args: TranslateArgMap): String;


"""

    out += methods()
    out += "}"

    return out

def write_source():
    source = build_source()
    path = Path(f"rsdroid/build/generated/source/fluent/anki/GeneratedTranslations.kt")
    if not path.parent.exists():
        path.parent.mkdir(parents=True)
    open(path, "w").write(source)


class Variable(TypedDict):
    name: str
    kind: Literal["Any", "Int", "String", "Float"]


def methods() -> str:
    out = []
    for module in modules:
        for translation in module["translations"]:
            key = stringcase.camelcase(translation["key"].replace("-", "_"))
            arg_types = get_arg_types(translation["variables"])
            args = get_args(translation["variables"])
            doc = translation["text"]
            out.append(
                f"""
    /** {doc} */
    fun {key}({arg_types}): String {{
        return translate({module["index"]}, {translation["index"]}, mapOf({args}))
    }}
"""
            )

    return "\n".join(out) + "\n"


def get_arg_types(args: list[Variable]) -> str:

    return ", ".join(
        [f"`{stringcase.camelcase(arg['name'])}`: {arg_kind(arg)}" for arg in args]
    )


def arg_kind(arg: Variable) -> str:
    if arg["kind"] == "Int":
        return "Int"
    elif arg["kind"] == "Any":
        return "TranslateArg"
    elif arg["kind"] == "Float":
        return "Double"
    else:
        return "String"


def get_args(args: list[Variable]) -> str:
    return ", ".join(
        [f'"{arg["name"]}" to asTranslateArg(`{stringcase.camelcase(arg["name"])}`)' for arg in args]
    )

ensure_i18n_module_correct()
write_source()
