import logging
import string

from gymnasium.utils.env_checker import check_env
from gymnasium.wrappers import TimeLimit
from py4j.java_gateway import JavaGateway, JavaObject
from stable_baselines3 import A2C
from stable_baselines3.common.evaluation import evaluate_policy

from skycatdev.rlmc.java.wrappers.skybridge_environment_wrapper import (
    WrappedSkybridgeEnvironment,
)


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = WrappedSkybridgeEnvironment(java_environment, get_gateway())
            env = TimeLimit(env, max_episode_steps=200)
            self.envs[java_environment] = env

        elif environment == "fight_skeleton":
            env = TimeLimit(
                WrappedSkybridgeEnvironment(java_environment, get_gateway()),
                max_episode_steps=400,
            )
            self.envs[java_environment] = env

    def train(self, environment: JavaObject):
        # check_env(self.envs[environment])
        agent = A2C("MultiInputPolicy", self.envs[environment], verbose=1)
        mean, std = evaluate_policy(agent, self.envs[environment], 10)
        print(f"mean={mean}, std={std}")
        agent.learn(5000)
        mean, std = evaluate_policy(agent, self.envs[environment], 10)
        print(f"mean={mean}, std={std}")
        agent.save("trained_agent")
        agent.learn(45000)
        agent.save("trained_agent2")

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]


logging.basicConfig(level=logging.INFO)

gateway = JavaGateway(
    start_callback_server=True, python_server_entry_point=Entrypoint(), auto_field=True
)
print("Gateway started")


def get_gateway():
    return gateway
