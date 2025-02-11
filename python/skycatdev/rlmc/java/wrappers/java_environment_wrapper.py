from abc import ABC, abstractmethod
from typing import Any, SupportsFloat

import gymnasium as gym
from gymnasium.core import ObsType, ActType
from py4j.java_gateway import JavaObject, JavaGateway

from skycatdev.rlmc.java.utils import java_map_to_dict


class WrappedJavaEnv(ABC, gym.Env):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        self.java_env = java_env
        self.java_view = java_gateway.new_jvm_view()

    def step(
        self, action: ActType
    ) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
        return self.unwrap_step(self.java_env.step(self.action_to_java(action)))

    def reset(
        self, seed: int | None = None, options: dict[str, Any] | None = None
    ) -> tuple[ObsType, dict[str, Any]]:
        super().reset(seed=seed)
        unwrapped = self.unwrap_reset(self.java_env.reset(seed, options))
        return unwrapped

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
            java_map_to_dict(step_tuple.info()),
        )

    def unwrap_reset(self, reset_tuple: JavaObject) -> tuple[ObsType, dict[str, Any]]:
        return self.obs_to_python(reset_tuple.observation()), java_map_to_dict(
            reset_tuple.info()
        )
