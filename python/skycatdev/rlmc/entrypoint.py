import logging
import string

from gymnasium.wrappers import TimeLimit
from py4j.java_gateway import JavaGateway, JavaObject
from stable_baselines3 import PPO
from stable_baselines3.common.evaluation import evaluate_policy

from skycatdev.rlmc.java.wrappers.wrapped_basic_player_environment import (
    WrappedBasicPlayerEnvironment,
)


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = WrappedBasicPlayerEnvironment(java_environment, get_gateway())
            env = TimeLimit(env, max_episode_steps=200)
            self.envs[java_environment] = env

        elif environment == "fight_skeleton":
            env = WrappedBasicPlayerEnvironment(java_environment, get_gateway())
            env = TimeLimit(env, max_episode_steps=400)
            self.envs[java_environment] = env

    def train(self, environment: JavaObject):
        # check_env(self.envs[environment])
        agent = PPO("MultiInputPolicy", self.envs[environment], verbose=1)
        mean, std = evaluate_policy(agent, self.envs[environment], 10)
        print(f"mean={mean}, std={std}")
        agent.learn(5000)
        mean, std = evaluate_policy(agent, self.envs[environment], 10)
        print(f"mean={mean}, std={std}")
        agent.save("trained_agent")
        agent.learn(45000)
        agent.save("trained_agent2")
        self.envs[environment].close()

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]


logging.basicConfig(level=logging.INFO)

gateway = JavaGateway(
    start_callback_server=True, python_server_entry_point=Entrypoint(), auto_field=True
)
print("Gateway started")


def get_gateway():
    return gateway
