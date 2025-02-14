#!/usr/bin/env sh
python3 python/skycatdev/rlmc/entrypoint.py & ./gradlew runServer && fg