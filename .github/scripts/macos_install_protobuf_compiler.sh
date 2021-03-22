# Fix for:
# protoc: stdout: . stderr: Traceback (most recent call last):
#    File "/Users/runner/work/Anki-Android-Backend/Anki-Android-Backend/./tools/protoc-gen/protoc-gen.py", line 8, in <module>
#      from google.protobuf.compiler import plugin_pb2 as plugin
#  ModuleNotFoundError: No module named 'google'
#  --anki_out: protoc-gen-anki: Plugin failed with status code 1.
#
# on macos-latest `pip3 install protobuf-compiler` fails with "src/python/grpcio/grpc/_cython/cygrpc.cpp:64654:42: error: no member named 'tp_print' in '_typeobject'"#
# so we use Python 3.7 to stop this (with pyenv)
# Add a timeout of ~3 minutes for this. A failure will hang.
#
#
brew install pyenv
git clone https://github.com/pyenv/pyenv.git ~/.pyenv
echo 'export PYENV_ROOT="$HOME/.pyenv"' >> ~/.bash_profile
echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> ~/.bash_profile
echo 'export GITHUB_PATH="$PYENV_ROOT/bin:$GITHUB_PATH"' >> ~/.bash_profile
echo 'eval "$(pyenv init -)"' >> ~/.bash_profile
source ~/.bash_profile
pyenv install 3.7.9
pyenv shell 3.7.9
pyenv global 3.7.9
python3 -v
sudo apt-get install python3-dev
pip3 install protobuf
pip3 install protobuf-compiler
.github/scripts/protoc_gen_deps.py
echo "Completed testing"

