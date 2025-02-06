import string, logging

from gymnasium.utils.env_checker import check_env
from gymnasium.wrappers import TimeLimit, FlattenObservation
from stable_baselines3 import A2C
from py4j.java_gateway import JavaGateway, JavaObject

from skybridge_environment_wrapper import WrappedSkybridgeEnvironment


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = WrappedSkybridgeEnvironment(java_environment, get_gateway())
            self.envs[java_environment] = env
    def train(self, environment: JavaObject):
        # check_env(self.envs[environment])
        agent = A2C("MultiInputPolicy", self.envs[environment])
        agent.learn(100)

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]

logging.basicConfig(level=logging.INFO)

gateway = JavaGateway(start_callback_server=True, python_server_entry_point=Entrypoint(), auto_field=True)
print("Gateway started")

def get_gateway():
    return gateway