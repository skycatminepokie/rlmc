import string, logging

from gymnasium.utils.env_checker import check_env
from gymnasium.wrappers import TimeLimit
from stable_baselines3 import A2C
from py4j.java_gateway import JavaGateway, JavaObject

from skycatdev.rlmc.skybridge_environment_wrapper import WrappedSkybridgeEnvironment


class Entrypoint(object):
    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = TimeLimit(WrappedSkybridgeEnvironment(java_environment, get_gateway()), max_episode_steps=400)
            check_env(env)
            agent = A2C("MlpPolicy", env)
            agent.learn(100)

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]

logging.basicConfig(level=logging.DEBUG)

gateway = JavaGateway(start_callback_server=True, python_server_entry_point=Entrypoint())
print("Gateway started")

def get_gateway():
    return gateway