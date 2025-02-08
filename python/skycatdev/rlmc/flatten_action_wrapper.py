from typing import SupportsFloat, Any

from gymnasium import ActionWrapper
from gymnasium.core import ObsType, Env, ActType, WrapperActType
from gymnasium.spaces import flatten_space, unflatten, Dict


class FlattenAction(ActionWrapper):
    def __init__(self, env: Env[ObsType, ActType]):
        super().__init__(env)
        self.action_space = flatten_space(env.action_space)
        if isinstance(env.action_space, Dict):
            self.key_to_index = {}
            self.index_to_key = {}
            i = 0
            for key in env.action_space.keys():
                self.key_to_index[key] = i
                self.index_to_key[i] = key
                i += 1

    def action(self, action: WrapperActType) -> ActType:
        return unflatten(self.env.action_space, action)

    def step(
        self, action: WrapperActType
    ) -> tuple[ObsType, SupportsFloat, bool, bool, dict[str, Any]]:
        return super().step(unflatten(self.env.action_space, action))

    def index_if_dict(self, action: WrapperActType) -> WrapperActType:
        if isinstance(self.env.action_space, Dict):
            ret = {}
            for k, v in enumerate(action.spaces.items()):
                ret[self.key_to_index.get(k)] = v
            return ret
        return action

    def unindex_if_dict(self, action: WrapperActType) -> WrapperActType:
        if isinstance(self.env.action_space, Dict):
            ret = {}
            for i, v in enumerate(action.spaces.items()):
                ret[self.index_to_key.get(i)] = v
            return ret
        return action
