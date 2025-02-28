import logging
import string
from typing import List

from gymnasium.wrappers import TimeLimit
from py4j.java_gateway import JavaGateway, JavaObject
from stable_baselines3 import PPO
from stable_baselines3.common.evaluation import evaluate_policy

from skycatdev.rlmc.java.wrappers.wrapped_basic_player_observation_environment import (
    WrappedBasicPlayerObservationEnvironment,
)


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = WrappedBasicPlayerObservationEnvironment(
                java_environment, get_gateway()
            )
            env = TimeLimit(env, max_episode_steps=200)
            self.envs[java_environment] = env

        elif environment == "fight_enemy":
            env = WrappedBasicPlayerObservationEnvironment(
                java_environment, get_gateway()
            )
            env = TimeLimit(env, max_episode_steps=400)
            self.envs[java_environment] = env

    def train(
        self,
        environment: JavaObject,
        episodes: int,
        save_path: str,
        load_path: str | None = None,
    ):
        if load_path is not None:
            agent = PPO.load(
                load_path, self.envs[environment], force_reset=True, verbose=1
            )
        else:
            agent = PPO("MultiInputPolicy", self.envs[environment], verbose=1)
        agent.learn(episodes)
        agent.save(save_path)
        self.envs[environment].close()

    def evaluate(self, environment: JavaObject, episodes: int, load_path: str) -> str:
        agent = PPO.load(load_path, self.envs[environment], force_reset=True, verbose=1)
        mean, std = evaluate_policy(agent, self.envs[environment], episodes, False)
        self.envs[environment].close()
        return f"Mean: {mean}, Std: {std}"

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]


logging.basicConfig(level=logging.INFO)

gateway = JavaGateway(
    start_callback_server=True, python_server_entry_point=Entrypoint(), auto_field=True
)
print("Gateway started")


def get_gateway():
    return gateway
