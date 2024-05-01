use anki_io::{copy_file, create_dir_all, read_dir_files};
use anki_process::CommandExt;
use anyhow::Result;
use camino::{Utf8Path, Utf8PathBuf};
use std::env::consts::OS;
use std::fs::File;
use std::io::{BufRead, BufReader, BufWriter, Write};
use std::path::{Path, PathBuf};
use std::process::Command;
use std::{env, io};

const ANDROID_OUT_DIR: &str = "rsdroid/build/generated/jniLibs";
const ROBOLECTRIC_OUT_DIR: &str = "rsdroid-testing/build/generated/jniLibs";

fn main() -> Result<()> {
    if env::var("RUNNING_FROM_BUILD_SCRIPT").is_ok() {
        return Ok(());
    }
    let ndk_path = Utf8PathBuf::from(env::var("ANDROID_NDK_HOME").unwrap_or_default());
    if !ndk_path.file_name().unwrap_or_default().starts_with("26.") {
        panic!("Expected ANDROID_NDK_HOME to point to a 26.x NDK. Future versions may work, but are untested.");
    }

    build_web_artifacts()?;
    build_android_jni()?;
    build_robolectric_jni()?;
    run_gradle()?;

    println!();
    println!("*** Build complete.");

    Ok(())
}

fn run_gradle() -> Result<()> {
    if env::var("RUNNING_FROM_GRADLE").is_err() {
        println!("*** Running gradle");
        let mut cmd = if cfg!(windows) {
            let mut cmd = Command::new("cmd");
            cmd.args(["/c", "gradlew.bat"]);
            cmd
        } else {
            Command::new("./gradlew")
        };
        cmd.env("RUNNING_FROM_BUILD_SCRIPT", "1")
            .args(["assembleRelease", "rsdroid-testing:build"])
            .ensure_success()?;
    }
    Ok(())
}

fn build_web_artifacts() -> Result<()> {
    println!("*** Building desktop web components");
    let artifacts_dir = Path::new("rsdroid/build/generated/anki_artifacts/backend");
    let mut cmd = if cfg!(windows) {
        let mut cmd = Command::new("cmd");
        cmd.args(["/c", "tools\\ninja.bat"]);
        cmd
    } else {
        Command::new("./ninja")
    };

    cmd.current_dir("anki")
        .args([
            "extract:protoc",
            "css:_root-vars",
            "ts:reviewer:reviewer.js",
            "ts:reviewer:reviewer.css",
            "ts:reviewer:reviewer_extras_bundle.js",
            "ts:reviewer:reviewer_extras.css",
            "qt:aqt:data:web:pages",
        ])
        .ensure_success()?;

    create_dir_all(artifacts_dir.join("web"))?;

    let web_dir = artifacts_dir.join("web");
    for file in read_dir_files("anki/out/qt/_aqt/data/web/pages")? {
        let file = file?;
        let path = file.path();
        copy_file(
            &path,
            web_dir.join(path.file_name().unwrap().to_str().unwrap()),
        )?;
    }
    copy_file(
        "anki/out/ts/reviewer/reviewer_extras_bundle.js",
        artifacts_dir.join("web/reviewer_extras_bundle.js"),
    )?;
    copy_file(
        "anki/out/ts/reviewer/reviewer_extras.css",
        artifacts_dir.join("web/reviewer_extras.css"),
    )?;
    copy_file(
        "anki/out/ts/reviewer/reviewer.js",
        artifacts_dir.join("web/reviewer.js"),
    )?;
    copy_file(
        "anki/out/ts/reviewer/reviewer.css",
        artifacts_dir.join("web/reviewer.css"),
    )?;
    copy_file(
        "anki/out/ts/lib/sass/_root-vars.css",
        artifacts_dir.join("web/root-vars.css"),
    )?;
    copy_file(
        "anki/cargo/licenses.json",
        artifacts_dir.join("web/licenses-cargo.json"),
    )?;
    copy_file(
        "anki/ts/licenses.json",
        artifacts_dir.join("web/licenses-ts.json"),
    )?;
    // Replaces "/_anki/" with `${ankidroid.postBaseUrl}_anki/` in postProto()
    // so the POSTs can be mapped to a different base url by changing the following line:
    // path = "/_anki/".concat(method); → path = `${ankidroid.postBaseUrl}_anki/`.concat(method);
    let _ = replace_string_in_file(
        &artifacts_dir.join("web/reviewer.js"),
        "\"/_anki/\"",
        "`${ankidroid.postBaseUrl}_anki/`",
    );
    let _ = replace_string_in_file(
        &artifacts_dir.join("web/reviewer_extras_bundle.js"),
        "\"/_anki/\"",
        "`${ankidroid.postBaseUrl}_anki/`",
    );
    Ok(())
}

fn build_android_jni() -> Result<()> {
    println!("*** Building Android JNI library + backend interface");
    let jni_dir = Path::new(ANDROID_OUT_DIR);
    if jni_dir.exists() {
        std::fs::remove_dir_all(jni_dir)?;
    }
    create_dir_all(jni_dir)?;

    let all_archs = env::var("ALL_ARCHS").is_ok();
    let ndk_targets = add_android_rust_targets(all_archs)?;
    let (is_release, _release_dir) = check_release(false);

    Command::run("cargo install cargo-ndk@3.3.0")?;

    let mut command = Command::new("cargo");
    command
        // build products go into separate folder so they don't trigger recompile
        // of robolectric/desktop code
        .env("CARGO_TARGET_DIR", "target")
        .env("STRINGS_JSON", env!("STRINGS_JSON_ANKIDROID"))
        .arg("ndk")
        .arg("-o")
        .arg(jni_dir)
        .args(ndk_targets)
        .args(["build", "-p", "rsdroid"]);
    if is_release {
        command.arg("--release");
    }
    command.ensure_success()?;

    Ok(())
}

// is_release, release/debug dir
// windows robolectric is forced to release, as debug builds fail with an error
fn check_release(force_release_on_windows: bool) -> (bool, &'static str) {
    if env::var("RELEASE").is_ok() || (force_release_on_windows && cfg!(windows)) {
        (true, "release")
    } else {
        (false, "debug")
    }
}

/// Returns target list to pass to cargo ndk
fn add_android_rust_targets(all_archs: bool) -> Result<&'static [&'static str]> {
    Ok(if all_archs {
        add_rust_targets(&[
            "armv7-linux-androideabi",
            "i686-linux-android",
            "aarch64-linux-android",
            "x86_64-linux-android",
        ])?;
        &[
            "-t",
            "armv7-linux-androideabi",
            "-t",
            "i686-linux-android",
            "-t",
            "aarch64-linux-android",
            "-t",
            "x86_64-linux-android",
        ]
    } else if cfg!(all(target_os = "macos", target_arch = "aarch64")) {
        add_rust_targets(&["aarch64-linux-android"])?;
        &["-t", "arm64-v8a"]
    } else {
        add_rust_targets(&["x86_64-linux-android"])?;
        &["-t", "x86_64"]
    })
}

fn add_rust_targets(targets: &[&str]) -> Result<()> {
    Command::new("rustup")
        .args(["target", "add"])
        .args(targets)
        .ensure_success()?;
    Ok(())
}

fn build_robolectric_jni() -> Result<()> {
    println!("*** Building Robolectric JNI library");
    let jni_dir = Path::new(ROBOLECTRIC_OUT_DIR);
    if jni_dir.exists() {
        std::fs::remove_dir_all(jni_dir)?;
    }
    create_dir_all(jni_dir)?;

    let all_archs = env::var("ALL_ARCHS").is_ok();
    let (is_release, release_dir) = check_release(true);
    let target_root = Utf8Path::new("anki/out/rust");
    let file_in_target =
        |platform: &str, fname: &str| target_root.join(platform).join(release_dir).join(fname);

    if all_archs {
        if cfg!(not(target_os = "macos")) {
            panic!("Must be on macOS to do a multi-arch build.");
        }

        let mac_targets = &["x86_64-apple-darwin", "aarch64-apple-darwin"];
        add_rust_targets(mac_targets)?;
        for target in mac_targets {
            build_rsdroid(is_release, target, target_root)?;
        }
        Command::new("lipo")
            .arg("-create")
            .args(&[
                file_in_target("x86_64-apple-darwin", "librsdroid.dylib"),
                file_in_target("aarch64-apple-darwin", "librsdroid.dylib"),
            ])
            .arg("-output")
            .arg(jni_dir.join("librsdroid.dylib"))
            .ensure_success()?;

        let linux_targets = &["x86_64-unknown-linux-gnu"];
        add_rust_targets(linux_targets)?;
        build_rsdroid(is_release, linux_targets[0], target_root)?;
        copy_file(
            file_in_target(linux_targets[0], "librsdroid.so"),
            jni_dir.join("librsdroid.so"),
        )?;

        let windows_targets = &["x86_64-pc-windows-gnu"];
        add_rust_targets(windows_targets)?;
        build_rsdroid(is_release, windows_targets[0], target_root)?;
        copy_file(
            file_in_target(windows_targets[0], "rsdroid.dll"),
            jni_dir.join("rsdroid.dll"),
        )?;
    } else {
        // Just build for current architecture
        build_rsdroid(is_release, "", target_root)?;
        let mut found_one = false;
        for fname in ["librsdroid.so", "librsdroid.dylib", "rsdroid.dll"] {
            let file = target_root.join(release_dir).join(fname);
            if Path::new(&file).exists() {
                found_one = true;
                copy_file(&file, jni_dir.join(fname))?;
            }
        }
        assert!(
            found_one,
            "expected to find at least one robolectric library"
        );
    }

    Ok(())
}

fn build_rsdroid(is_release: bool, target_arch: &str, target_dir: &Utf8Path) -> Result<()> {
    let mut command = Command::new("cargo");
    command
        // Robolectric build cache can be shared with desktop, as it's the same arch
        .env("CARGO_TARGET_DIR", target_dir)
        .env("STRINGS_JSON", env!("STRINGS_JSON_ANKIDROID"))
        .args(["build", "-p", "rsdroid"]);
    if is_release {
        command.arg("--release");
    }
    if !target_arch.is_empty() {
        command.args(["--target", target_arch]);
    }
    if OS == "macos" && target_arch == "x86_64-unknown-linux-gnu" {
        command.env("CC", "x86_64-unknown-linux-gnu-gcc").env(
            "CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER",
            "x86_64-unknown-linux-gnu-gcc",
        );
    }
    command.ensure_success()?;
    Ok(())
}

fn replace_string_in_file(file_path: &PathBuf, old_word: &str, new_word: &str) -> io::Result<()> {
    let input_file = File::open(file_path)?;
    let reader = BufReader::new(input_file);

    let temp_file_path = file_path.with_extension("tmp");
    let temp_file = File::create(&temp_file_path)?;
    let mut writer = BufWriter::new(temp_file);

    for line in reader.lines() {
        let line = line?;
        let modified_line = line.replace(old_word, new_word);
        writeln!(writer, "{}", modified_line)?;
    }

    writer.flush()?;

    std::fs::rename(&temp_file_path, file_path)?;

    Ok(())
}
