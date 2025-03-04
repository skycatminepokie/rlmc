from gymnasium.core import ObsType
from py4j.java_gateway import JavaObject, JavaGateway

from skycatdev.rlmc.java.wrappers.vec3d import Vec3d
from skycatdev.rlmc.java.wrappers.wrapped_basic_player_observation_environment import (
    WrappedBasicPlayerObservationEnvironment,
)


class WrappedFightEnemyEnvironment(WrappedBasicPlayerObservationEnvironment):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        super().__init__(java_env, java_gateway)
        self.max_enemy_distance = self.java_env.getMaxEnemyDistance()

    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        basic_obs = super().obs_to_python(java_obs)
        assert isinstance(basic_obs, dict)
        enemy = Vec3d(java_obs.getVecToEnemy())
        basic_obs["enemy"] = enemy
        return basic_obs
