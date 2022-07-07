$SRC_DIR = (Get-Item .).FullName
$ANKI_SRC = (Get-Item $SRC_DIR/rslib-bridge/anki).FullName
$TEMP_DIR =  (Get-Item $ENV:Temp).FullName

New-Item -P $SRC_DIR/rsdroid/build/generated/anki_artifacts/web -itemType Directory
$BUILD_DIR = (Get-Item $SRC_DIR/rsdroid/build/generated/anki_artifacts).FullName

Set-Location -Path $ANKI_SRC
bazel build --symlink_prefix=$TEMP_DIR/.bazel/ -c opt buildinfo.txt --verbose_failures
Copy-Item -Path $TEMP_DIR/.bazel/bin/buildinfo.txt -Destination $BUILD_DIR/buildinfo.txt -PassThru

bazel build ts/reviewer/reviewer_extras_bundle --symlink_prefix=$TEMP_DIR/.bazel/

Set-Location -Path $ANKI_SRC/qt/aqt/data/web/pages
bazel build pages --symlink_prefix=$TEMP_DIR/.bazel/ 

Copy-Item -Path "$TEMP_DIR\.bazel\bin\qt\aqt\data\web\pages\*" -Destination $BUILD_DIR/web -Recurse -PassThru
Copy-Item -Path $TEMP_DIR/.bazel/bin/ts/reviewer/reviewer_extras_bundle.js -Destination $BUILD_DIR/web -PassThru
