use std::{fs, path::Path};

use anki_io::read_to_string;
use inflections::Inflect;
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct Variable {
    name: String,
    kind: String,
}

#[derive(Debug, Deserialize)]
struct Translation {
    key: String,
    index: usize,
    variables: Vec<Variable>,
    text: String,
}

#[derive(Debug, Deserialize)]
struct Module {
    index: usize,
    translations: Vec<Translation>,
}

fn get_strings() -> Vec<Module> {
    let data = read_to_string(std::env::var("STRINGS_JSON_ANKIDROID").unwrap()).unwrap();
    serde_json::from_str(&data).unwrap()
}

fn methods(modules: &[Module]) -> String {
    let mut out = vec![];
    for module in modules {
        for translation in &module.translations {
            let key = translation.key.replace('-', "_").to_camel_case();
            let arg_types = get_arg_types(&translation.variables);
            let args = get_args(&translation.variables);
            out.push(format!(
                "
    /** {} */
    fun {}({}): String {{
        return translate({}, {}, mapOf({}))
    }}
",
                translation.text, key, arg_types, module.index, translation.index, args
            ));
        }
    }
    out.join("\n") + "\n"
}

/// Rewrites Fluent placeables (`{$deck_name}`) into Kotlin string templates
/// (`${`deckName`}`), using the same parameter names as `get_arg_types`.
fn kotlin_template_text(translation: &Translation) -> String {
    translation
        .variables
        .iter()
        .fold(translation.text.clone(), |text, var| {
            text.replace(
                &format!("{{${}}}", var.name),
                &format!("${{`{}`}}", var.name.to_camel_case()),
            )
        })
}

// Similar with methods() but the generated methods return the backend strings directy
fn static_text_methods(modules: &[Module]) -> String {
    let mut out = vec![];
    for module in modules {
        for translation in &module.translations {
            let key = translation.key.replace('-', "_").to_camel_case();
            let arg_types = get_arg_types(&translation.variables);
            out.push(format!(
                "
    /** {} */
    override fun {}({}): String {{
        return \"\"\"{}\"\"\"
    }}
",
                translation.text,
                key,
                arg_types,
                kotlin_template_text(translation)
            ));
        }
    }
    out.join("\n") + "\n"
}

fn get_arg_types(args: &[Variable]) -> String {
    args.iter()
        .map(|arg| format!("`{}`: {}", arg.name.to_camel_case(), arg_kind(&arg.kind)))
        .collect::<Vec<String>>()
        .join(", ")
}

fn arg_kind(kind: &str) -> &str {
    match kind {
        "Int" => "Int",
        "Any" => "TranslateArg",
        "Float" => "Double",
        _ => "String",
    }
}

fn get_args(args: &[Variable]) -> String {
    args.iter()
        .map(|arg| {
            format!(
                "\"{}\" to asTranslateArg(`{}`)",
                arg.name,
                arg.name.to_camel_case()
            )
        })
        .collect::<Vec<String>>()
        .join(", ")
}

fn build_source(modules: &[Module]) -> String {
    let mut out = String::from(
        "// Copyright: Ankitects Pty Ltd and contributors
// License: GNU AGPL, version 3 or later; http://www.gnu.org/licenses/agpl.html

package anki.i18n;

import anki.i18n.TranslateArgValue

fun asTranslateArg(arg: Any): TranslateArgValue {
    val builder = TranslateArgValue.newBuilder()
    when (arg) {
        is String -> builder.setStr(arg)
        is Int -> builder.setNumber(arg.toDouble())
        is Double -> builder.setNumber(arg)
        else -> throw Exception(\"invalid arg provided to translation\")
    }
    return builder.build()
}

// This should be either String, Double or Int
typealias TranslateArg = Any

typealias TranslateArgMap = Map<String, TranslateArgValue>

interface GeneratedTranslations {
    fun translate(module: Int, translation: Int, args: TranslateArgMap): String;


",
    );

    out.push_str(&methods(modules));
    out.push('}');
    out.push_str("

/** Note: This class is used for tooling and should not be used directly */
object PreviewTranslations : GeneratedTranslations {
    override fun translate(module: Int, translation: Int, args: Map<String, TranslateArgValue>): String = \"Unused\"

"
    );
    out.push_str(&static_text_methods(modules));
    out.push('}');

    out
}

pub fn write_translations() {
    let modules = get_strings();
    let source = build_source(&modules);
    let folder = Path::new(env!("GENERATED_BACKEND_DIR"));
    let path = folder.join("GeneratedTranslations.kt");
    fs::create_dir_all(folder).unwrap();
    fs::write(path, source).unwrap();
}
