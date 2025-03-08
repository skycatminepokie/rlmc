import logging
import string
import sys
from typing import override

from gymnasium.wrappers import TimeLimit
from py4j.java_gateway import JavaGateway, JavaObject, server_connection_started
from stable_baselines3 import PPO, A2C
from stable_baselines3.common.evaluation import evaluate_policy
from stable_baselines3.common.monitor import Monitor
from stable_baselines3.common.vec_env import DummyVecEnv, VecNormalize

from skycatdev.rlmc.java.wrappers.wrapped_basic_player_observation_environment import (
    WrappedBasicPlayerObservationEnvironment,
)
from skycatdev.rlmc.java.wrappers.wrapped_fight_enemy_environment import (
    WrappedFightEnemyEnvironment,
)


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            env = WrappedBasicPlayerObservationEnvironment(
                java_environment, get_gateway()
            )
            env = Monitor(env)
            env = TimeLimit(env, max_episode_steps=200)
            self.envs[java_environment] = env

        elif environment == "fight_enemy":
            env = WrappedFightEnemyEnvironment(java_environment, get_gateway())
            env = TimeLimit(env, max_episode_steps=400)
            env = Monitor(env)
            env = DummyVecEnv([lambda: env])
            env = VecNormalize(env, norm_reward=True, norm_obs=False)
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
            agent = A2C(
                "MultiInputPolicy",
                self.envs[environment],
                verbose=1,
                tensorboard_log="./tensorboard_log/",
                ent_coef=0.7,
            )
        agent.learn(episodes, tb_log_name=save_path)
        agent.save(save_path)
        self.envs[environment].close()

    def evaluate(self, environment: JavaObject, episodes: int, load_path: str) -> str:
        agent = PPO.load(load_path, self.envs[environment], force_reset=True, verbose=1)
        mean, std = evaluate_policy(agent, self.envs[environment], episodes, False)
        self.envs[environment].close()
        return f"Mean: {mean}, Std: {std}"

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]


gateway = JavaGateway(
    start_callback_server=True, python_server_entry_point=Entrypoint(), auto_field=True
)


class Log4jHandler(logging.Handler):
    @override
    def emit(self, record):
        if record.name.startswith("py4j"):
            return  # otherwise Py4j causes infinite recursion
        message = self.format(record)
        try:
            gateway.jvm.com.skycatdev.rlmc.Rlmc.pythonLog(record.levelname, message)
        except RecursionError as e:
            print(e, file=sys.stderr)
            print(
                "RecursionError caught while printing, please report. Printing message via stdout.",
                file=sys.stderr,
            )
            print(message)


class Log4jStream:
    def write(self, message):
        message = message.strip()
        if message:
            gateway.jvm.com.skycatdev.rlmc.Rlmc.pythonLog(
                "INFO", f"STDOUT/ERR: {message}"
            )

    def flush(self):
        pass


# Pretty sure you need the parameters for the connection
# noinspection PyUnusedLocal
def connection_started(sender, **kwargs):
    base_logger = logging.getLogger()
    base_logger.setLevel(logging.DEBUG)
    log4j_handler = Log4jHandler()
    log4j_handler.setFormatter(logging.Formatter("%(message)s"))
    base_logger.addHandler(log4j_handler)

    sys.stdout = Log4jStream()
    sys.stderr = Log4jStream()


server_connection_started.connect(
    connection_started, sender=gateway.get_callback_server()
)

print("Gateway started")


def get_gateway():
    return gateway
