RLMC uses a lot of dependencies and tools to work.

## For Users
You need:
- Minecraft 1.21.1
- Fabric Loader
- Fabric API
- Carpet

## For Everyone Else

### All (Non-Transitive) Dependencies
- [Python 3](https://www.python.org/) ([Python License](https://docs.python.org/3/license.html)) because I need the tooling built for it
- [Java Temurin](https://adoptium.net/) ([GNU General Public License, version 2 with the Classpath Exception](https://adoptium.net/docs/faq/)) because half the stuff is built on it
- [Py4J](https://www.py4j.org/) ([BSD 3-Clause](https://github.com/py4j/py4j/blob/master/LICENSE.txt)) is used to communicate between Java and Python
- [stable-baselines3](https://github.com/DLR-RM/stable-baselines3) ([MIT](https://github.com/DLR-RM/stable-baselines3/blob/master/LICENSE)) is used to create and train models
- [Spotless](https://github.com/diffplug/spotless) ([Apache 2.0](https://github.com/diffplug/spotless/blob/main/LICENSE.txt)) and the [Spotless Gradle](https://plugins.jetbrains.com/plugin/18321-spotless-gradle) ([MIT](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)) plugin for IntelliJ help keep code clean
- [Checker Framework](https://checkerframework.org/) ([GPLv2 with classpath exception](https://github.com/typetools/checker-framework/blob/master/LICENSE.txt)) to apply @NotNull by default
- [Gymnasium](https://gymnasium.farama.org/) ([MIT](https://github.com/Farama-Foundation/Gymnasium/blob/main/LICENSE)) as the basis for RL
- [Fabric Loader](https://github.com/FabricMC/fabric-loader) ([Apache 2.0](https://github.com/FabricMC/fabric-loader/blob/master/LICENSE)) to load the mod
- [Fabric API](https://github.com/FabricMC/fabric) ([Apache 2.0](https://github.com/FabricMC/fabric/blob/1.21.1/LICENSE)) for a lot of needed Minecraft hooks
- [Carpet](https://github.com/gnembon/fabric-carpet) ([MIT](https://github.com/gnembon/fabric-carpet/blob/master/LICENSE)) for controlling players
- [Minecraft](https://www.minecraft.net/en-us) (ARR) for Minecraft itself
- [Docker](https://www.docker.com/) (IDK man) for reliable testing

### References
- [checkerframework-gradle-plugin](https://github.com/kelloggm/checkerframework-gradle-plugin) for info on adding Checker Framework dependency
- [CraftGround]()