set root=%~dp0\..\..
set build=%root%\rsdroid\build\generated\anki_artifacts
set web=%build%\web
if not exist %web% (
    mkdir %web%
)

cd anki
call tools\ninja extract:protoc ts:reviewer:reviewer_extras_bundle.js qt/aqt:data/web/pages || exit /b 1

copy out\ts\reviewer\reviewer_extras_bundle.js %web% || exit /b 1
copy out\qt\_aqt\data\web\pages\*.* %web% || exit /b 1
copy cargo\licenses.json %web% || exit /b 1
copy ts\licenses.json %web% || exit /b 1

