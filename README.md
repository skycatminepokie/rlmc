# RLMC

## Development Setup
### Script (CLI)
1. Install Python and Java 21
2. Checkout the repository
3. Navigate into the repository
4. `chmod +x start.sh`
5. `chmod +x setup.sh`
6. `chmod +x gradlew`
7. `./setup.sh`
8. Whenever you're ready, run `start.sh`
9. Note that it will leave a hanging python process when the server stops (this should be fixed later).

### IntelliJ
1. Install the Python Community plugin
2. Checkout the repository.
3. Open the project in IntelliJ, let it load, then close the project
4. Open the project in IntelliJ again (this is how Fabric mods set up run configs)
5. Create a venv for the python folder
6. Install the requirements from `requirments.txt` by opening the file, then opening the Tools tab and clicking "Sync Python requirements..."

### Docker
I've got a Dockerfile! It should work. Make an [issue](https://github.com/skycatminepokie/rlmc/issues/new) if it doesn't.
1. Checkout the repository
2. Navigate into the root project folder
3. Build and run the Dockerfile

### Troubleshooting