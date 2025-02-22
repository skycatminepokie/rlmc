import numpy as np
from gymnasium.spaces import Dict, Box
from py4j.java_gateway import JavaObject, JVMView

from skycatdev.rlmc.java.wrappers import block_pos, direction
from skycatdev.rlmc.java.wrappers.block_pos import BlockPos, MAX_BLOCK_DISTANCE
from skycatdev.rlmc.java.wrappers.direction import Direction

SPACE = Dict({"pos": block_pos.SPACE, "side": direction.SPACE})


class WrappedBlockHitResult(object):
    def __init__(self, block_hit_result: JavaObject):
        self._block_pos = BlockPos(block_hit_result.blockPos())
        self._side = Direction.from_java(block_hit_result.side())
        self._block_state = block_hit_result.blockState()

    @property
    def block_pos(self) -> BlockPos:
        return self._block_pos

    @property
    def side(self) -> int:
        return self._side

    def to_array(self, java_view: JVMView) -> list[int]:
        return [
            self._block_pos.get_x(),
            self._block_pos.get_y(),
            self._block_pos.get_z(),
            self._side,
            self.get_state_id(java_view),
        ]

    def get_state_id(self, java_view: JVMView) -> int:
        return java_view.com.skycatdev.rlmc.Rlmc.getBlockStateMap().get(
            self._block_state
        )


def flat_space(num_hit_results: int, num_block_states: int) -> Box:
    # Block x, y, z, direction, num of results
    assert num_hit_results > 0, "num_hit_results must be an integer greater than zero"
    return Box(
        low=np.tile(
            [-MAX_BLOCK_DISTANCE, -MAX_BLOCK_DISTANCE, -MAX_BLOCK_DISTANCE, 0, 0],
            (num_hit_results, 1),
        ),
        high=np.tile(
            [
                MAX_BLOCK_DISTANCE,
                MAX_BLOCK_DISTANCE,
                MAX_BLOCK_DISTANCE,
                5,
                num_block_states - 1,
            ],
            (num_hit_results, 1),
        ),
        shape=(num_hit_results, 5),
        dtype=np.int64,
    )
