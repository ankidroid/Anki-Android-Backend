@ECHO OFF
set root=%~dp0\..\..
set PYTHONPATH=%root%\anki\pylib\anki\_vendor
set PATH=%root%\anki\out\extracted\protoc\bin;%PATH%
%root%\anki\out\pyenv\scripts\python "%~dp0\protoc-gen.py"