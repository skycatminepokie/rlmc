from typing import Callable

from gymnasium import Space
from gymnasium.core import ObsType
from gymnasium.spaces import Dict
from py4j.java_gateway import JavaObject, JavaGateway
from typing_extensions import override

from skycatdev.rlmc.wrappers import vec3d
from skycatdev.rlmc.wrappers.vec3d import Vec3d
from skycatdev.rlmc.wrappers.wrapped_basic_player_observation_environment import (
    WrappedBasicPlayerObservationEnvironment,
)


class WrappedFightEnemyEnvironment(WrappedBasicPlayerObservationEnvironment):
    def __init__(
        self,
        java_env: JavaObject | Callable[[], JavaObject],
        java_gateway: JavaGateway | Callable[[], JavaGateway],
    ):
        super().__init__(java_env, java_gateway)

    @override
    def make_observation_space(self) -> Space:
        self.observation_dict["enemy"] = vec3d.space(
            self.java_env.getMaxEnemyDistance(),
            self.java_env.getMaxEnemyDistance(),
            self.java_env.getMaxEnemyDistance(),
        )
        return Dict(self.observation_dict)

    @override
    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        basic_obs = super().obs_to_python(java_obs)
        assert isinstance(basic_obs, dict)
        enemy = Vec3d(java_obs.getVecToEnemy())
        basic_obs["enemy"] = enemy.to_array()
        return basic_obs
