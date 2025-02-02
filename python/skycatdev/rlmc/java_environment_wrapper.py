from abc import ABC, abstractmethod
from typing import Any, SupportsFloat

import gymnasium as gym
from gymnasium.core import ObsType, ActType
from numpy import array, ndarray
from py4j.java_collections import JavaMap, JavaList
from py4j.java_gateway import JavaObject, JavaGateway


class WrappedJavaEnv(ABC, gym.Env):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        self.java_env = java_env
        self.java_view = java_gateway.new_jvm_view()

    def step(
        self, action: ActType
    ) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
        return self.unwrap_step(self.java_env.step())

    def reset(
        self, seed: int | None = None, options: dict[str, Any] | None = None
    ) -> tuple[ObsType, dict[str, Any]]:
        return self.unwrap_reset(self.java_env.reset())

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


def java_map_to_dict(java_map: JavaMap) -> dict:
    dictionary = {}
    for key in java_map.keys():
        dictionary[key] = java_map.get(key)
    return dictionary


def java_list_to_array(java_list: JavaList) -> ndarray:
    return array([e for e in java_list])


class WrappedBlockHitResult(object):
    def __init__(self, block_hit_result: JavaObject):
        self.block_pos = block_hit_result.getBlockPos()
        self.side = block_hit_result.getSide()

    def get_block_pos(self) -> JavaObject:
        return self.block_pos

    def get_side(self) -> JavaObject:
        return self.side

    def to_dict(self, world: JavaObject) -> dict:
        return {
            "block": world.getBlockState(self.block_pos),
            "x": self.block_pos.getX(),
            "y": self.block_pos.getY(),
            "z": self.block_pos.getZ(),
        }


class WrappedItemStack(object):
    def __init__(self, item_stack: JavaObject):
        self.item_stack = item_stack

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.item_stack.getItem().getIdAsString(),
            "count": self.item_stack.getCount(),
        }
