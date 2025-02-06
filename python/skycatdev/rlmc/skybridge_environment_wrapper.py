import numpy as np
from gymnasium.core import ActType, ObsType
from gymnasium.spaces import Text, Discrete, Box, Dict, Sequence, MultiDiscrete
from py4j.java_collections import JavaList
from py4j.java_gateway import JavaObject, JavaGateway, java_import

from skycatdev.rlmc.java_environment_wrapper import (
    WrappedJavaEnv,
    WrappedBlockHitResult,
    java_list_to_array,
)

MAX_ID_LENGTH = 32767

MAX_BLOCK_DISTANCE = 3000    # 30_000_554

MAX_STACK_SIZE = 999


class WrappedSkybridgeEnvironment(WrappedJavaEnv):
    def __init__(self, java_env: JavaObject, java_gateway: JavaGateway):
        super().__init__(java_env, java_gateway)
        java_import(self.java_view, "com.skycatdev.rlmc.environment.FutureActionPack")
        java_import(self.java_view, "carpet.helpers.EntityPlayerActionPack")
        item_space = Dict({"id" : Text(MAX_ID_LENGTH), "count" : Discrete(MAX_STACK_SIZE)})
        self.action_space = MultiDiscrete([2, 2, 2, 2, 2, 2, 2, 2, 2, 9])
        # {
        #     "attack" : Discrete(2),
        #     "use" : Discrete(2),
        #     "forward" : Discrete(2),
        #     "left" : Discrete(2),
        #     "backward" : Discrete(2),
        #     "right" : Discrete(2),
        #     "sprint" : Discrete(2),
        #     "sneak" : Discrete(2),
        #     "jump" : Discrete(2),
        #     "yaw" : Box(0, 360), # TODO: Normalize
        #     "pitch" : Box(-90, 90), # TODO: Normalize
        #     "hotbar" : Discrete(9)
        # }
        self.observation_space = Dict({
            # "blocks" : Sequence(
            #     Dict({
            #         "block" : Text(MAX_ID_LENGTH),
            #         "x" : Discrete(MAX_BLOCK_DISTANCE, start=-MAX_BLOCK_DISTANCE),
            #         "y" : Discrete(MAX_BLOCK_DISTANCE, start=-MAX_BLOCK_DISTANCE),
            #         "z" : Discrete(MAX_BLOCK_DISTANCE, start=-MAX_BLOCK_DISTANCE),
            #     })
            # ),
            "x" : Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE),
            "y" : Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE),
            "z" : Box(-MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE),
            "yaw" : Box(-180, 180, dtype=np.float32), # TODO: Normalize
            "pitch" : Box(-90, 90, dtype=np.float32), # TODO: Normalize
            "hotbar" : Discrete(9),
            # "inventory" : Dict({
            #     "main" : Sequence(item_space),
            #     "armor" : Sequence(item_space),
            #     "offhand":item_space,
            # }),
            # "history" : self.action_space
        })

    def obs_to_python(self, java_obs: JavaObject) -> ObsType:
        assert isinstance(
            java_obs.blocks(), JavaList
        ), f"Expected JavaList, got {type(java_obs.blocks())}"
        agent = java_obs.self()
        return {
            # "blocks" : [
            #     WrappedBlockHitResult(hit_result).to_dict(agent.getWorld())
            #     for hit_result in java_obs.blocks()
            # ],
            "x" : [agent.getX()],
            "y" : [agent.getY()],
            "z" : [agent.getZ()],
            "yaw" : [self.java_view.net.minecraft.util.math.MathHelper.wrapDegrees(
                agent.getYaw()
            )],
            "pitch" : [self.java_view.net.minecraft.util.math.MathHelper.wrapDegrees(
                agent.getPitch()
            )],
            "hotbar" : agent.getInventory().selectedSlot
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

        # action_pack.setYaw(action["yaw"]) TODO add these in
        # action_pack.setPitch(action["pitch"])
        assert isinstance(action[9], np.int64)
        action_pack.setHotbar(action[9].item())

        return action_pack

    def action_to_python(self, action: JavaObject) -> ActType:
        action_types = action.getActions()
        python_action = [0,0,0,0,0,0,0,0,0]
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

        # python_action["yaw"] = action.getYaw()
        # python_action["pitch"] = action.getPitch() TODO: put this back in
        python_action[9] = action.getHotbar()

        return python_action
