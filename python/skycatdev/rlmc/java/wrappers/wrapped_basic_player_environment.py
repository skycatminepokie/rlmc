import numpy as np
from gymnasium.core import ActType, ObsType
from gymnasium.spaces import (
    Discrete,
    Box,
    Dict,
    MultiDiscrete,
    flatten,
)
from py4j.java_collections import JavaList
from py4j.java_gateway import JavaObject, JavaGateway, java_import

from skycatdev.rlmc.java.utils import java_list_to_array
from skycatdev.rlmc.java.wrappers import block_hit_result, entity_hit_result
from skycatdev.rlmc.java.wrappers.block_hit_result import WrappedBlockHitResult
from skycatdev.rlmc.java.wrappers.block_pos import MAX_BLOCK_DISTANCE
from skycatdev.rlmc.java.wrappers.entity_hit_result import WrappedEntityHitResult
from skycatdev.rlmc.java.wrappers.java_environment_wrapper import WrappedJavaEnv

MAX_ID_LENGTH = 32767

MAX_STACK_SIZE = 999


class WrappedBasicPlayerEnvironment(WrappedJavaEnv):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        super().__init__(java_env, java_gateway)
        self.raycasts = self.java_env.getRaycasts()
        java_import(self.java_view, "com.skycatdev.rlmc.environment.FutureActionPack")
        java_import(self.java_view, "carpet.helpers.EntityPlayerActionPack")
        # attack, use, forward, left, backward, right, sprint, sneak, jump, hotbar yaw, pitch
        self.action_space = MultiDiscrete([2, 2, 2, 2, 2, 2, 2, 2, 2, 9, 360, 180])
        self.block_space = block_hit_result.flat_space(
            self.raycasts,
            self.java_view.com.skycatdev.rlmc.Rlmc.getBlockStateMap().size(),
        )
        self.flat_entity_space, self.entity_space = entity_hit_result.space_of(
            self.raycasts,
            self.java_view.com.skycatdev.rlmc.Rlmc.getEntityTypeMap().size(),
        )
        self.observation_space = Dict(
            {
                "blocks": self.block_space,
                "x": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
                "y": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
                "z": Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, shape=(1,)),
                "yaw": Box(-180, 180, dtype=np.float32, shape=(1,)),  # TODO: Normalize
                "pitch": Box(-90, 90, dtype=np.float32, shape=(1,)),  # TODO: Normalize
                "hotbar": Discrete(9),
                "entities": self.flat_entity_space,
                # "inventory" : Dict({
                #     "main" : Sequence(item_space),
                #     "armor" : Sequence(item_space),
                #     "offhand":item_space,
                # }),
                # "history" : self.action_space
            }
        )

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
        return {
            "blocks": np.array(
                tuple(
                    hit_result.to_array(self.java_view)
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
            ],
            "pitch": [
                self.java_view.net.minecraft.util.math.MathHelper.wrapDegrees(
                    agent.getPitch()
                )
            ],
            "hotbar": agent.getInventory().selectedSlot,
            "entities": flatten(self.entity_space, entities),
            # "inventory" : {
            #     "main": java_list_to_array(agent.getInventory().main),
            #     "armor": java_list_to_array(agent.getInventory().armor),
            #     "offhand": java_list_to_array(agent.getInventory.offHand),
            # },
            # "history" : [
            #     self.action_to_python(e) for e in java_list_to_array(java_obs.history)
            # ],
        }

    def action_to_java(self, action: ActType) -> JavaObject:
        action_pack = self.java_view.FutureActionPack()

        if action[0] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.ATTACK)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.ATTACK)

        if action[1] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.USE)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.USE)

        if action[2] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.FORWARD)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.FORWARD)

        if action[3] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.LEFT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.LEFT)

        if action[4] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.BACKWARD)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.BACKWARD)

        if action[5] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.RIGHT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.RIGHT)

        if action[6] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.SPRINT)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.SPRINT)

        if action[7] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.SNEAK)
        else:
            action_pack.add(self.java_view.FutureActionPack.ActionType.SNEAK)

        if action[8] == 1:
            action_pack.add(self.java_view.FutureActionPack.ActionType.JUMP)
        else:
            action_pack.remove(self.java_view.FutureActionPack.ActionType.JUMP)

        action_pack.setHotbar(action[9].item())

        action_pack.setYaw(float(action[10]))
        action_pack.setPitch(float(action[11]))

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
