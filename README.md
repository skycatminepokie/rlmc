# RLMC

## Development Setup
### Manual
1. Checkout the repository. If you're using IntelliJ, also use the Python Community plugin.
2. Navigate into the repository
3. Create a `conda` environment (or a venv)
4. Install the requirements from `requirments.txt` using `conda install --yes --file python/requirements.txt`
5. Open the project in IntelliJ, let it load, then close the project
6. Open the project in IntelliJ again (this is how Fabric mods set up run configs)

### Docker
I've got a Dockerfile! It should work. Make an [issue](https://github.com/skycatminepokie/rlmc/issues/new) if it doesn't.
1. Checkout the repository
2. Navigate into the root project folder
3. Run the Dockerfile