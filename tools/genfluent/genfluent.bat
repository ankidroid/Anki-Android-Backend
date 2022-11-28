@ECHO OFF
set root=%~dp0\..\..
set PYTHONPATH=%root%\anki\pylib\anki\_vendor
python "%~dp0\genfluent.py"