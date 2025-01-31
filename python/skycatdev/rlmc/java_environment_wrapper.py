from typing import Tuple, Dict, Any, SupportsFloat

from gymnasium.core import ObsType
from py4j.java_collections import JavaMap
from py4j.java_gateway import JavaObject


class JavaEnv(object):
    def __init__(self, java_env: JavaObject):
        self.java_env = java_env

    def step(self, action: JavaObject) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
        return self.get_step_info(self.java_env.step(action))

    def get_step_info(self, step_info: JavaObject) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
      return java_map_to_dict(step_info.observation()), step_info.reward(), step_info.terminated(), step_info.truncated(), java_map_to_dict(step_info.info())

    def get_reset_info(self, reset_info: JavaObject) -> tuple[ObsType, Dict[str, Any]]:
        return reset_info.observation(), java_map_to_dict(reset_info.info())

def java_map_to_dict(java_map: JavaMap) -> Dict:
    dictionary = {}
    for key in java_map.keys():
        dictionary[key] = java_map.get(key)
    return dictionary
