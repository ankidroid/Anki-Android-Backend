# wsl does not support bazel yet https://github.com/bazelbuild/bazel/issues/10941
echo "Renaming Bash in WSL"
mv.exe "C:/WINDOWS/system32/bash.EXE" "C:/WINDOWS/system32/bash_copy.EXE"
Set-Alias -Name bash -Value "C:/msys64/usr/bin/bash.exe"
$env:BAZEL_SH = "C:/msys64/usr/bin/bash.exe"
[Environment]::SetEnvironmentVariable('BAZEL_SH', 'C:/msys64/usr/bin/bash.exe', 'Machine')
