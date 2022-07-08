Set-Alias -Name bash -Value "C:/msys64/usr/bin/bash.exe"
$env:BAZEL_SH = "C:/msys64/usr/bin/bash.exe" 

$SRC_DIR = (Get-Item .).FullName
$ROOT_DIR = (Get-Item D:/a/).FullName
$ANKI_SRC = (Get-Item $SRC_DIR/rslib-bridge/anki).FullName
$TEMP_DIR = (Get-Item $ENV:TEMP).FullName

New-Item -P $SRC_DIR/rsdroid/build/generated/anki_artifacts/web -itemType Directory
$BUILD_DIR = (Get-Item $SRC_DIR/rsdroid/build/generated/anki_artifacts).FullName

Set-Location -Path $ANKI_SRC

bazel --output_user_root=$ROOT_DIR/_bzl build -c opt buildinfo.txt --symlink_prefix=$TEMP_DIR/.bazel/ --verbose_failures
Copy-Item -Path $TEMP_DIR/.bazel/bin/buildinfo.txt -Destination $BUILD_DIR/buildinfo.txt -PassThru

bazel --output_user_root=$ROOT_DIR/_bzl build ts/reviewer/reviewer_extras_bundle --symlink_prefix=$TEMP_DIR/.bazel/ --verbose_failures
Copy-Item -Path $TEMP_DIR/.bazel/bin/ts/reviewer/reviewer_extras_bundle.js -Destination $BUILD_DIR/web -PassThru

bazel --output_user_root=$ROOT_DIR/_bzl build qt/aqt/data/web/pages --symlink_prefix=$TEMP_DIR/.bazel/ --verbose_failures
Get-Childitem $TEMP_DIR/.bazel/bin/qt/aqt/data/web/pages -Recurse -Include *.html,*.js,*.css | Copy-Item -Destination $BUILD_DIR/web -PassThru
