import gymnasium.spaces
from py4j.java_gateway import JavaObject

from block_pos import BlockPos
from skycatdev.rlmc.java.wrappers import block_pos

SPACE = gymnasium.spaces.Dict({"pos": block_pos.SPACE})


class WrappedBlockHitResult(object):
    def __init__(self, block_hit_result: JavaObject):
        self.block_pos = BlockPos(block_hit_result.getBlockPos())
        self.side = block_hit_result.getSide()

    def get_block_pos(self) -> BlockPos:
        return self.block_pos

    def get_side(self) -> JavaObject:
        return self.side  # TODO

    def to_dict(self) -> dict:
        return {
            # TODO "block": world.getBlockState(self.block_pos),
            "pos": self.block_pos.to_dict()
        }
