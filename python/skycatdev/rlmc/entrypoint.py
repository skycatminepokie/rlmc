import logging
import string
import sys
import warnings
from typing import Any
from typing import override

from gymnasium.wrappers import TimeLimit, FrameStackObservation
from py4j.java_collections import JavaArray
from py4j.java_gateway import JavaGateway, JavaObject, server_connection_started
from stable_baselines3 import PPO, A2C
from stable_baselines3.common.callbacks import (
    BaseCallback,
    EvalCallback,
    StopTrainingOnRewardThreshold,
    CallbackList,
)
from stable_baselines3.common.evaluation import evaluate_policy
from stable_baselines3.common.logger import HParam
from stable_baselines3.common.monitor import Monitor
from stable_baselines3.common.on_policy_algorithm import OnPolicyAlgorithm
from stable_baselines3.common.vec_env import DummyVecEnv

from skycatdev.rlmc.java.wrappers.wrapped_basic_player_observation_environment import (
    WrappedBasicPlayerObservationEnvironment,
)
from skycatdev.rlmc.java.wrappers.wrapped_fight_enemy_environment import (
    WrappedFightEnemyEnvironment,
)


class HParamCallback(
    BaseCallback
):  # from https://stable-baselines3.readthedocs.io/en/master/guide/tensorboard.html
    """
    Saves the hyperparameters and metrics at the start of the training, and logs them to TensorBoard.
    """

    def _on_training_start(self) -> None:
        hparam_dict = {
            "algorithm": self.model.__class__.__name__,
            "learning rate": self.model.learning_rate,
        }
        if isinstance(self.model, OnPolicyAlgorithm):
            hparam_dict["entropy coefficient"] = self.model.ent_coef
            hparam_dict["gamma"] = self.model.gamma
            if "net_arch" in self.model.policy_kwargs:
                hparam_dict["net_arch"] = str(self.model.policy_kwargs["net_arch"])
        # define the metrics that will appear in the `HPARAMS` Tensorboard tab by referencing their tag
        # Tensorboard will find & display metrics from the `SCALARS` tab
        metric_dict = {
            "rollout/ep_len_mean": 0,
            "train/value_loss": 0.0,
        }
        self.logger.record(
            "hparams",
            HParam(hparam_dict, metric_dict),
            exclude=("stdout", "log", "json", "csv"),
        )

    def _on_step(self) -> bool:
        return True


class Entrypoint(object):
    envs = {}

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        env_settings: JavaObject = java_environment.getSettings()
        frame_stack: int = env_settings.getFrameStack()
        time_limit: int = env_settings.getTimeLimit()

        if environment == "skybridge":
            env = WrappedBasicPlayerObservationEnvironment(
                java_environment, get_gateway()
            )
        elif environment == "fight_enemy":
            env = WrappedFightEnemyEnvironment(java_environment, get_gateway())
        else:
            assert (
                environment == "go_north"
            ), f"Received unknown environment type {environment}."
            env = WrappedBasicPlayerObservationEnvironment(
                java_environment, get_gateway()
            )

        if frame_stack > 1:
            env = FrameStackObservation(env, frame_stack)
        if time_limit > 0:
            env = TimeLimit(env, max_episode_steps=time_limit)
        if env_settings.shouldUseMonitor():
            env = Monitor(env)
        env = DummyVecEnv([lambda: env])
        self.envs[java_environment] = env

    # noinspection PyPep8Naming
    def runKwargs(self, environment: JavaObject, ees: JavaObject):
        save_path: str | None = ees.getSavePath()
        load_path: str | None = ees.getLoadPath()
        episodes: int = ees.getEpisodes()
        tensorboard_log_name: str | None = ees.getTensorboardLogName()
        tensorboard_path: str = ees.getTensorboardLogPath() or "./tensorboard_log/"
        algorithm_str: str = ees.getAlgorithm()
        algorithm: OnPolicyAlgorithm
        load = load_path is not None
        net_arch: JavaArray | None = ees.getNetArch()
        policy_kwargs: dict[str, Any] = {}
        if net_arch is not None:
            policy_kwargs["net_arch"] = list(net_arch)
        if load:
            if algorithm_str == "A2C":
                algorithm = A2C.load(
                    load_path,
                    self.envs[environment],
                    tensorboard_log=tensorboard_path,
                    policy_kwargs=policy_kwargs,
                )
            elif algorithm_str == "PPO":
                algorithm = PPO.load(
                    load_path,
                    self.envs[environment],
                    tensorboard_log=tensorboard_path,
                    policy_kwargs=policy_kwargs,
                )
            else:
                warnings.warn("Tried to load algorithm with invalid name. Aborting.")
                return
        else:
            if algorithm_str == "A2C":
                algorithm = A2C(
                    "MultiInputPolicy",
                    self.envs[environment],
                    tensorboard_log=tensorboard_path,
                    policy_kwargs=policy_kwargs,
                )
            elif algorithm_str == "PPO":
                algorithm = PPO(
                    "MultiInputPolicy",
                    self.envs[environment],
                    tensorboard_log=tensorboard_path,
                    policy_kwargs=policy_kwargs,
                    batch_size=ees.getBatchSize() or 64,
                )
            else:
                warnings.warn("Tried to create algorithm with invalid name. Aborting.")
                return
        # load params
        if ees.getGamma() is not None:
            algorithm.gamma = ees.getGamma()
        if ees.getEntCoef() is not None:
            algorithm.ent_coef = ees.getEntCoef()
        if ees.getGaeLambda() is not None:
            algorithm.gae_lambda = ees.getGaeLambda()
        if ees.getVfCoef() is not None:
            algorithm.vf_coef = ees.getVfCoef()
        if ees.getMaxGradNorm() is not None:
            algorithm.max_grad_norm = ees.getMaxGradNorm()
        if ees.getLearningRate() is not None:
            algorithm.learning_rate = ees.getLearningRate()
        if ees.getNSteps() is not None:
            algorithm.n_steps = ees.getNSteps()

        if ees.isTraining():
            eval_callback = EvalCallback(
                eval_env=self.envs[environment.makeAnother().get().get()],
                best_model_save_path="./logs/best_model/",
                log_path="./logs/",
                eval_freq=10_000,
                deterministic=True,
                callback_on_new_best=StopTrainingOnRewardThreshold(0.75),
            )
            h_param_callback = HParamCallback()
            callback = CallbackList([eval_callback, h_param_callback])
            if tensorboard_log_name is None:
                algorithm.learn(
                    episodes,
                    callback=callback,
                    reset_num_timesteps=not load,
                )
            else:
                algorithm.learn(
                    episodes,
                    tb_log_name=tensorboard_log_name,
                    callback=callback,
                    reset_num_timesteps=not load,
                )
        else:
            evaluate_policy(algorithm, self.envs[environment], n_eval_episodes=episodes)

        if save_path is not None:
            algorithm.save(save_path)

        self.envs[environment].close()

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


class Log4jStream:  # TODO: I think this stopped working
    # noinspection PyMethodMayBeStatic
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
