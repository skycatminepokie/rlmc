import numpy as np
from gymnasium.spaces import Dict, Box
from py4j.java_gateway import JavaObject

from skycatdev.rlmc.java.wrappers import block_pos, direction
from skycatdev.rlmc.java.wrappers.block_pos import BlockPos, MAX_BLOCK_DISTANCE
from skycatdev.rlmc.java.wrappers.direction import Direction

SPACE = Dict({"pos": block_pos.SPACE, "side": direction.SPACE})


class WrappedBlockHitResult(object):
    def __init__(self, block_hit_result: JavaObject):
        self.block_pos = BlockPos(block_hit_result.getBlockPos())
        self.side = Direction.from_java(block_hit_result.getSide())

    def get_block_pos(self) -> BlockPos:
        return self.block_pos

    def get_side(self) -> int:
        return self.side

    def to_dict(self) -> dict:
        return {
            # TODO "block": world.getBlockState(self.block_pos),
            "pos": self.block_pos.to_dict(),
            "side": self.get_side(),
        }


def flat_space(num_hit_results: int) -> Box:
    # Block x, y, z, direction, num of results
    assert num_hit_results > 0, "num_hit_results must be an integer greater than zero"
    return Box(
        low=np.tile(
            [-MAX_BLOCK_DISTANCE, -MAX_BLOCK_DISTANCE, -MAX_BLOCK_DISTANCE, 0],
            (num_hit_results, 1),
        ),
        high=np.tile(
            [MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, MAX_BLOCK_DISTANCE, 5],
            (num_hit_results, 1),
        ),
        shape=(num_hit_results, 4),
        dtype=np.int64,
    )
