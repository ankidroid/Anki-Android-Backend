# on macos-latest `pip3 install protobuf-compiler` fails with "src/python/grpcio/grpc/_cython/cygrpc.cpp:64654:42: error: no member named 'tp_print' in '_typeobject'"#
# so we use Python 3.7 to stop this (with pyenv)
# Add a timeout of ~3 minutes for this. A failure will hang.
brew install pyenv
git clone https://github.com/pyenv/pyenv.git ~/.pyenv
echo 'export PYENV_ROOT="$HOME/.pyenv"' >> ~/.bash_profile
echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> ~/.bash_profile
exec "$SHELL"
pyenv install 3.7.9
pyenv shell 3.7.9
python3 -v
pip3 install protobuf-compiler
.github/scripts/protoc_gen_deps.py
