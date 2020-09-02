if not exist "%~dp0\out" mkdir "%~dp0\out"
protoc --include_source_info --plugin=protoc-gen-anki="..\protoc-gen.bat" --anki_out="out" backend.proto