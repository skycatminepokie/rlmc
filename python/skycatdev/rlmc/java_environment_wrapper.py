from abc import ABC, abstractmethod
from typing import Any, SupportsFloat

import gymnasium as gym
from gymnasium.core import ObsType, ActType
from py4j.java_gateway import JavaObject


class WrappedJavaEnv(ABC, gym.Env):
    def __init__(self, java_env: JavaObject):
        self.java_env = java_env

    def step(
        self, action: ActType
    ) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
        return self.unwrap_step(self.java_env.step())

    def reset(
        self, seed: int | None = None, options: dict[str, Any] | None = None
    ) -> tuple[ObsType, dict[str, Any]]:
        return self.unwrap_reset(self.java_env.reset())

    @abstractmethod
    def obs_to_java(self, obs: ObsType) -> JavaObject:
        pass

    @abstractmethod
    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        pass

    @abstractmethod
    def action_to_java(self, action: ActType) -> JavaObject:
        pass

    @abstractmethod
    def action_to_python(self, action: JavaObject) -> ActType:
        pass

    def unwrap_step(
        self, step_tuple: JavaObject
    ) -> tuple[ObsType, float, bool, bool, dict[str, Any]]:
        return (
            self.obs_to_python(step_tuple.observation()),
            step_tuple.reward(),
            step_tuple.terminated(),
            step_tuple.truncated(),
            step_tuple.info(),
        )

    def unwrap_reset(self, reset_tuple: JavaObject) -> tuple[ObsType, dict[str, Any]]:
        return self.obs_to_python(reset_tuple.observation()), reset_tuple.info()
