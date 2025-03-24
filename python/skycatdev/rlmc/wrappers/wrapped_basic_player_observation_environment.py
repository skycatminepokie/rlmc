from typing import Callable

import numpy as np
from gymnasium import Space
from gymnasium.core import ActType, ObsType
from gymnasium.spaces import (
    Discrete,
    Box,
    Dict,
    flatten,
)
from py4j.java_collections import JavaList, JavaMap
from py4j.java_gateway import JavaObject, JavaGateway, java_import

from skycatdev.rlmc.utils import java_list_to_array
from skycatdev.rlmc.wrappers import block_hit_result, entity_hit_result
from skycatdev.rlmc.wrappers.block_hit_result import WrappedBlockHitResult
from skycatdev.rlmc.wrappers.block_pos import MAX_BLOCK_DISTANCE, BlockPos
from skycatdev.rlmc.wrappers.entity_hit_result import WrappedEntityHitResult
from skycatdev.rlmc.wrappers.java_environment_wrapper import WrappedJavaEnv

MAX_ID_LENGTH = 32767

MAX_STACK_SIZE = 999


class WrappedBasicPlayerObservationEnvironment(WrappedJavaEnv):
    def __init__(
        self,
        java_env: JavaObject | Callable[[], JavaObject],
        java_gateway: JavaGateway | Callable[[], JavaGateway],
    ):
        super().__init__(java_env, java_gateway)
        self.raycasts = self.java_env.getRaycasts()
        self.raycast_distance = self.java_env.getRaycastDistance()
        java_import(self.java_view, "com.skycatdev.rlmc.environment.FutureActionPack")
        java_import(self.java_view, "carpet.helpers.EntityPlayerActionPack")
        # attack, use, forward, left, backward, right, sprint, sneak, jump, hotbar yaw, pitch
        self.action_space = Box(
            np.array([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1]),
            np.array([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]),
            dtype=np.float32,
        )
        self.block_space = block_hit_result.flat_space(
            self.raycasts,
            self.java_view.com.skycatdev.rlmc.Rlmc.getBlockStateMap().size(),
            self.raycast_distance,
        )
        self.flat_entity_space, self.entity_space = entity_hit_result.space_of(
            self.raycasts,
            self.java_view.com.skycatdev.rlmc.Rlmc.getEntityTypeMap().size(),
        )
        # Attack, use, forward, left, backward, right, sprint, sneak, jump, yaw, pitch
        self.history_space = Box(
            np.append(np.tile(-1200, 9), np.array([-1, -1])),
            np.append(np.tile(1200, 9), np.array([1, 1])),
            dtype=np.float32,
            shape=(11,),
        )
        self.observation_dict = {
            "blocks": self.block_space,
            "x": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
            "y": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
            "z": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
            "yaw": Box(-1, 1, dtype=np.float32, shape=(1,)),
            "pitch": Box(-1, 1, dtype=np.float32, shape=(1,)),
            "hotbar": Discrete(9),
            "entities": self.flat_entity_space,
            # "inventory" : Dict({
            #     "main" : Sequence(item_space),
            #     "armor" : Sequence(item_space),
            #     "offhand":item_space,
            # }),
            "history": self.history_space,
            "health": Box(-1, 1, dtype=np.float32, shape=(1,)),
        }
        self.observation_space = self.make_observation_space()

    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        assert isinstance(
            java_obs.blocks(), JavaList
        ), f"Expected JavaList, got {type(java_obs.blocks())}"
        agent = java_obs.self()
        block_hit_results = tuple(
            WrappedBlockHitResult(hit_result)
            for hit_result in java_list_to_array(java_obs.blocks())
        )
        entity_hit_results = tuple(
            WrappedEntityHitResult(hit_result, self.java_view)
            for hit_result in java_list_to_array(java_obs.entities())
        )
        entities = entity_hit_result.to_space_dict(
            entity_hit_results,
            self.java_view,
            self.java_view.com.skycatdev.rlmc.Rlmc.getEntityTypeMap().size(),
        )
        history = self.history_to_python(java_obs)
        return {
            "blocks": np.array(
                tuple(
                    hit_result.to_array(self.java_view, BlockPos(agent.getBlockPos()))
                    for hit_result in block_hit_results
                )
            ),
            "x": [agent.getX()],
            "y": [agent.getY()],
            "z": [agent.getZ()],
            "yaw": [
                self.java_view.net.minecraft.util.math.MathHelper.wrapDegrees(
                    agent.getYaw()
                )
                / 180
            ],
            "pitch": [
                self.java_view.net.minecraft.util.math.MathHelper.wrapDegrees(
                    agent.getPitch()
                )
                / 90
            ],
            "hotbar": agent.getInventory().selectedSlot,
            "entities": flatten(self.entity_space, entities),
            # "inventory" : {
            #     "main": java_list_to_array(agent.getInventory().main),
            #     "armor": java_list_to_array(agent.getInventory().armor),
            #     "offhand": java_list_to_array(agent.getInventory.offHand),
            # },
            "history": history,
            "health": [(np.clip(agent.getHealth(), 0.0, 20.0) - 10) / 10],
        }

    def history_to_python(self, java_obs):
        history_map = java_obs.history().getActionHistory()
        assert isinstance(history_map, JavaMap)
        attack = history_map.get(self.java_view.FutureActionPack.ActionType.ATTACK)
        use = history_map.get(self.java_view.FutureActionPack.ActionType.USE)
        forward = history_map.get(self.java_view.FutureActionPack.ActionType.FORWARD)
        left = history_map.get(self.java_view.FutureActionPack.ActionType.LEFT)
        backward = history_map.get(self.java_view.FutureActionPack.ActionType.BACKWARD)
        right = history_map.get(self.java_view.FutureActionPack.ActionType.RIGHT)
        sprint = history_map.get(self.java_view.FutureActionPack.ActionType.SPRINT)
        sneak = history_map.get(self.java_view.FutureActionPack.ActionType.SNEAK)
        jump = history_map.get(self.java_view.FutureActionPack.ActionType.JUMP)
        history = np.array(
            [
                attack if attack is not None else 0,
                use if use is not None else 0,
                forward if forward is not None else 0,
                left if left is not None else 0,
                backward if backward is not None else 0,
                right if right is not None else 0,
                sprint if sprint is not None else 0,
                sneak if sneak is not None else 0,
                jump if jump is not None else 0,
                java_obs.history().getYaw() / 180,
                java_obs.history().getPitch() / 90,
            ]
        )
        return history

    def action_to_java(self, action: ActType) -> JavaObject:
        action_pack = self.java_view.FutureActionPack()

        if round(action[0]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.ATTACK)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.ATTACK)

        if round(action[1]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.USE)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.USE)

        if round(action[2]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.FORWARD)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.FORWARD)

        if round(action[3]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.LEFT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.LEFT)

        if round(action[4]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.BACKWARD)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.BACKWARD)

        if round(action[5]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.RIGHT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.RIGHT)

        if round(action[6]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.SPRINT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.SPRINT)

        if round(action[7]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.SNEAK)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.SNEAK)

        if round(action[8]) == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.JUMP)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.JUMP)

        action_pack.setHotbar(round(action[9].item() * 8))

        action_pack.setYaw(float(action[10] * 180))
        action_pack.setPitch(float(action[11] * 90))

        return action_pack

    def action_to_python(self, action: JavaObject) -> ActType:
        action_types = action.getActions()
        python_action = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        assert isinstance(
            action_types, JavaList
        ), f"Expected a JavaList, got a(n) {type(action_types)}"
        for action_type in action_types:
            if action_type == self.java_view.FutureActionPack.ActionType.ATTACK:
                python_action[0] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.USE:
                python_action[1] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.FORWARD:
                python_action[2] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.LEFT:
                python_action[3] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.BACKWARD:
                python_action[4] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.RIGHT:
                python_action[5] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.SPRINT:
                python_action[6] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.SNEAK:
                python_action[7] = 1
            elif action_type == self.java_view.FutureActionPack.ActionType.JUMP:
                python_action[8] = 1

        python_action[9] = action.getHotbar()
        python_action[11] = int(action.getYaw())
        python_action[12] = int(action.getPitch())

        return python_action

    def make_observation_space(self) -> Space:
        return Dict(self.observation_dict)
