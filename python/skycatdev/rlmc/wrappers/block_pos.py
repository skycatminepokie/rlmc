import gymnasium.spaces
import numpy
import numpy as np
from py4j.java_gateway import JavaObject

MAX_BLOCK_DISTANCE = 3000  # TODO: Increase to 30_000_554
SPACE = gymnasium.spaces.Box(
    low=-MAX_BLOCK_DISTANCE, high=MAX_BLOCK_DISTANCE, shape=(3,), dtype=np.int32
)


class BlockPos(object):
    def __init__(self, java_block_pos: JavaObject):
        self.java_block_pos = java_block_pos

    def to_dict(self) -> dict[str, int]:
        return {"x": self.get_x(), "y": self.get_y(), "z": self.get_z()}

    def get_x(self) -> int:
        return self.java_block_pos.getX()

    def get_y(self) -> int:
        return self.java_block_pos.getY()

    def get_z(self) -> int:
        return self.java_block_pos.getZ()

    def to_array(self) -> numpy.ndarray:
        array = numpy.ndarray((3,), numpy.int32)
        array[0] = self.get_x()
        array[1] = self.get_y()
        array[2] = self.get_z()
        return array


def flat_sequence_space(size: int) -> gymnasium.spaces.Box:
    return gymnasium.spaces.Box(
        low=-MAX_BLOCK_DISTANCE,
        high=MAX_BLOCK_DISTANCE,
        shape=(size, 3),
        dtype=np.int32,
    )
