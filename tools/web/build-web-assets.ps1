$SRC_DIR = (Get-Item .).FullName
$ROOT_DIR = (Get-Item D:/a/).FullName
$ANKI_SRC = (Get-Item $SRC_DIR/anki).FullName
$TEMP_DIR = (Get-Item $ENV:TEMP).FullName

New-Item -P $SRC_DIR/rsdroid/build/generated/anki_artifacts/web -itemType Directory
$BUILD_DIR = (Get-Item $SRC_DIR/rsdroid/build/generated/anki_artifacts).FullName

Set-Location -Path $ANKI_SRC

tools\ninja extract:protoc ts:reviewer:reviewer_extras_bundle.js qt/aqt:data/web/pages

Copy-Item -Path out/ts/reviewer/reviewer_extras_bundle.js -Destination $BUILD_DIR/web -PassThru
Get-Childitem out/qt/_aqt/data/web/pages -Recurse -Include *.html,*.js,*.css | Copy-Item -Destination $BUILD_DIR/web -PassThru
Copy-Item -Path $ANKI_SRC/cargo/licenses.json -Destination $BUILD_DIR/web/licenses-cargo.json -PassThru
Copy-Item -Path $ANKI_SRC/ts/licenses.json -Destination $BUILD_DIR/web/licenses-ts.json -PassThru
