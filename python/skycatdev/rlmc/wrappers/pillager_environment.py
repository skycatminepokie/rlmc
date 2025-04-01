import numpy as np
from gymnasium.spaces import Box
from gymnasium.core import ActType, ObsType
from py4j.java_gateway import JavaObject, JavaGateway, JVMView, java_import
from sympy.codegen.ast import float32

from skycatdev.rlmc.wrappers import vec3d
from skycatdev.rlmc.wrappers.java_environment_wrapper import WrappedJavaEnv
from skycatdev.rlmc.wrappers.vec3d import Vec3d


class PillagerEnv(WrappedJavaEnv):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        super().__init__(java_env, java_gateway)
        java_import(
            self.java_view,
            "com.skycatdev.rlmc.environment.pillager.PillagerEnvironment",
        )
        java_import(
            self.java_view,
            "com.skycatdev.rlmc.environment.pillager.PillagerEnvironment.Action",
        )
        java_import(
            self.java_view,
            "com.skycatdev.rlmc.environment.pillager.PillagerEnvironment.Observation",
        )
        java_import(
            self.java_view,
            "com.skycatdev.rlmc.environment.pillager.PillagerEnvironment.PillagerGoal",
        )
        java_import(
            self.java_view,
            "com.skycatdev.rlmc.environment.pillager.PillagerEnvironment.PillagerGoal.CrossbowState",
        )

        movement_low: np.ndarray = np.array([-180, -90, 0])
        movement_high: np.ndarray = np.array([180, 90, 10])
        jump_low: np.ndarray = np.array([0])
        jump_high: np.ndarray = np.array([1])
        attack_low: np.ndarray = np.array([0])
        attack_high: np.ndarray = np.array([2])
        self.action_space = Box(
            np.concatenate((movement_low, jump_low, attack_low), dtype=np.float32),
            np.concatenate((movement_high, jump_high, attack_high), dtype=np.float32),
            shape=(5,),
            dtype=np.float32,
        )

        golem_vec_low: np.ndarray = np.array([-180, 90, 0])
        golem_vec_high: np.ndarray = np.array([180, 90, 100])
        crossbow_state_low: np.ndarray = np.array([0])
        crossbow_state_high: np.ndarray = np.array([2])
        self.observation_space = Box(
            np.concatenate((golem_vec_low, crossbow_state_low), dtype=np.float32),
            np.concatenate((golem_vec_high, crossbow_state_high), dtype=np.float32),
            shape=(4,),
            dtype=np.float32,
        )

    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        rot_vec: Vec3d = Vec3d(java_obs.rotVecToGolem())
        state: int = java_obs.crossbowState().getIndex()
        return np.concatenate((rot_vec.to_array(), [state]), dtype=np.float32)

    def action_to_java(self, action: ActType) -> JavaObject:
        move_vec: JavaObject = Vec3d.create_java(
            float(action[0]), float(action[1]), float(action[2]), self.java_view
        )
        return self.java_view.Action.withVec3dLook(
            move_vec,
            move_vec,
            bool(round(float(action[3]))),
            None,
            self.java_view.com.skycatdev.rlmc.environment.pillager.PillagerEnvironment.PillagerGoal.CrossbowAttack.fromIndex(
                float(action[4])
            ),
        )

    def action_to_python(self, action: JavaObject) -> ActType:
        if action.movement() is not None:
            vec: np.ndarray = Vec3d(action.movement()).to_array()
        else:
            vec: np.ndarray = np.array([0, 0, 0])
        jump: bool = action.jump()
        attack: int = action.attack().getIndex()
        return np.concatenate((vec, [jump], [attack]), dtype=np.float32)
