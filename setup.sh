#!/bin/bash
set -e
chmod +x gradlew
mkdir run
echo eula=true > run/eula.txt
python3 -m venv .venv
source .venv/bin/activate
pip install -r python/requirements.txt
cd python
pip install -e .
