#!/usr/bin/env bash

echo -e "\e[1;33m>>> Building Linux Image\e[0m (x86_64-unknown-linux-gnu) "
docker build -t ankidroid/rslib-bridge:linux . -f ./rslib-bridge/Dockerfiles/Dockerfile.linux



echo -e "\e[1;33m>>> Building Windows Image\e[0m (x86_64-pc-windows-gnu)"
docker build -t ankidroid/rslib-bridge:windows . -f ./rslib-bridge/Dockerfiles/Dockerfile.windows