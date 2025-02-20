#!/bin/bash
set -e
source .venv/bin/activate
cd python
python -m skycatdev.rlmc.entrypoint &
cd ..
./gradlew runServer
fg