from types import NoneType

from numpy import ndarray, array
from py4j.java_collections import JavaMap, JavaList
from py4j.java_gateway import JVMView


def java_map_to_dict(java_map: JavaMap) -> dict:
    dictionary = {}
    for key in java_map.keys():
        dictionary[key] = java_map.get(key)
    return dictionary


def java_list_to_array(java_list: JavaList) -> ndarray:
    return array([e for e in java_list])


def dict_to_java_map(dictionary: dict | None, java_view: JVMView) -> JavaMap | None:
    if isinstance(dictionary, NoneType):
        return None
    my_map = java_view.java.util.HashMap()
    for k in dictionary.keys():
        my_map.put(k, dictionary[k])
    return my_map
