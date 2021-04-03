python --version
git clone https://github.com/pyenv/pyenv.git ~/.pyenv
echo 'export PYENV_ROOT="$HOME/.pyenv"' >> ~/.bash_profile
echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> ~/.bash_profile
echo 'export GITHUB_PATH="$PYENV_ROOT/bin:$GITHUB_PATH"' >> ~/.bash_profile
echo 'eval "$(pyenv init -)"' >> ~/.bash_profile
source ~/.bash_profile
echo "Installing Python 3.7.9"
pyenv install 3.7.9
pyenv shell 3.7.9
pyenv global 3.7.9
python --version
pip3 install setuptools
pip3 install protobuf-compiler
.github/scripts/protoc_gen_deps.py