import numpy
from gymnasium.spaces import Box
import numpy as np
from py4j.java_gateway import JavaObject


class Vec3d:
    def __init__(
        self,
        vec3d: JavaObject | None = None,
        x: float = None,
        y: float = None,
        z: float = None,
    ):
        """
        Wrap a Java Vec3d if provided, or make a Vec3d from provided floats.
        :param vec3d: A Java Vec3d. If this is provided, the remaining arguments are ignored.
        :param x: A float. If this is provided, y and z must be provided as well.
        :param y: A float. If this is provided, x and z must be provided as well.
        :param z: A float. If this is provided, x and y must be provided as well.
        """
        if isinstance(vec3d, JavaObject) and vec3d is not None:
            self._x = vec3d.getX()
            self._y = vec3d.getY()
            self._z = vec3d.getZ()
        else:
            assert (
                isinstance(x, float) and isinstance(y, float) and isinstance(z, float)
            ), f"All x/y/z parameters must be float if no Java Vec3d is provided. Provided: vec3d={type(vec3d)}, x={type(x)}, y={type(y)}, z={type(z)}"
            self._x = x
            self._y = y
            self._z = z

    @property
    def x(self):
        return self._x

    @property
    def y(self):
        return self._y

    @property
    def z(self):
        return self._z

    def to_array(self) -> np.ndarray:
        return np.array([self.x, self.y, self.z])


def space(max_x: float, max_y: float, max_z: float) -> Box:
    return Box(
        np.array([-max_x, -max_y, -max_z]), np.array([max_x, max_y, max_z]), (3,)
    )
