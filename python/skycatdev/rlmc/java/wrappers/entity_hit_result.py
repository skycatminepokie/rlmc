from typing import Any

import numpy as np
from gymnasium.spaces import Dict, Box, Text, flatten_space
from py4j.java_gateway import JavaObject, JVMView

from skycatdev.rlmc.java.wrappers.block_pos import MAX_BLOCK_DISTANCE
from skycatdev.rlmc.java.wrappers.vec3d import Vec3d

MAX_IDENTIFIER_LENGTH = 32767


class WrappedEntityHitResult(object):
    def __init__(self, entity_hit_result: JavaObject | None, java_view: JVMView):
        """
        Wraps a Java EntityHitResult, or initializes an empty one if entity_hit_result is None.
        :param entity_hit_result:
        :param java_view:
        """
        if isinstance(entity_hit_result, JavaObject):
            self._pos = Vec3d(entity_hit_result.getPos())
            self._entity = java_view.net.minecraft.entity.EntityType.getId(
                entity_hit_result.getEntity().getType()
            ).toString()
        else:
            self._pos = Vec3d(x=float(0), y=float(0), z=float(0))
            self._entity = "rlmc:none"

    @property
    def pos(self) -> Vec3d:
        return self._pos

    @property
    def entity(self) -> str:
        return self._entity

    def to_dict(self) -> dict:
        return {"entity": self.entity, "pos": self._pos.to_array()}

    def to_array(self) -> np.ndarray:
        return np.array([self.pos.x, self.pos.y, self.pos.z, self.entity])


def space_of(num_hit_results: int) -> tuple[Box, Dict]:
    assert num_hit_results > 0, "num_hit_results must be an integer greater than zero"
    top_dict = {}
    for i in range(num_hit_results):
        top_dict[f"{i}"] = Dict(
            {
                "pos": Box(
                    -MAX_BLOCK_DISTANCE,
                    MAX_BLOCK_DISTANCE,
                    shape=(3,),
                    dtype=np.float64,
                ),
                "entity": Text(
                    min_length=3,
                    max_length=MAX_IDENTIFIER_LENGTH,
                    charset="abcdefghijklmnopqrstuvwxyz0123456789_-:",
                ),
            }
        )
    top_space = Dict(top_dict)
    return flatten_space(top_space), top_space


def to_dict(hit_results: tuple) -> dict[str, Any]:
    ret = {}
    for i, hit_result in enumerate(hit_results):
        ret[f"{i}"] = hit_result.to_dict()

    return ret
