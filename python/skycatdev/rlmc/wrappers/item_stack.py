from typing import Any

from py4j.java_gateway import JavaObject


class WrappedItemStack(object):
    def __init__(self, item_stack: JavaObject):
        self.item_stack = item_stack

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.item_stack.getItem().getIdAsString(),
            "count": self.item_stack.getCount(),
        }
