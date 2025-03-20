import gymnasium.spaces
from py4j.java_gateway import JavaObject

SPACE = gymnasium.spaces.Discrete(6)


class Direction:
    DOWN, UP, NORTH, SOUTH, WEST, EAST = range(6)

    @classmethod
    def from_java(cls, direction: JavaObject) -> int:
        return direction.getId()
