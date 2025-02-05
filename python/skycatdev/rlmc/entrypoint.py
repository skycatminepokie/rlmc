import string, logging

from py4j.java_gateway import JavaGateway, JavaObject

from skycatdev.rlmc.skybridge_environment_wrapper import WrappedSkybridgeEnvironment


class Entrypoint(object):
    def __init__(self, java_gateway: JavaGateway):
        self.java_gateway = java_gateway

    # noinspection PyPep8Naming
    def connectEnvironment(self, environment: string, java_environment: JavaObject):
        if environment == "skybridge":
            WrappedSkybridgeEnvironment(java_environment, self.java_gateway)

    class Java:
        implements = ["com.skycatdev.rlmc.PythonEntrypoint"]

logging.basicConfig(level=logging.DEBUG)

gateway = JavaGateway(start_callback_server=True)
gateway.python_server_entry_point = Entrypoint(gateway)
print("Gateway started")