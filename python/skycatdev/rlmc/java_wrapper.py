from py4j.java_collections import JavaMap


def java_map_to_dict(java_map: JavaMap) -> dict:
    dictionary = {}
    for key in java_map.keys():
        dictionary[key] = java_map.get(key)
    return dictionary
