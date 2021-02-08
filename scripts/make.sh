#!/bin/bash

rm -rf out

./scripts/make-jar.sh "$@"

# Copy HTTP Assets
mkdir -p out/www
cp -R www/* out/www/.

# Copy SSL Certs
mkdir -p out/ssl
cp -R ssl/out/* out/ssl/.

# Copy Config
mkdir -p out/conf
cp -R conf/* out/conf/.

# Create Run-Script
./scripts/make-scripts.sh

cp scripts/forward-ports.sh out/.
