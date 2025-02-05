package com.skycatdev.rlmc;

import com.skycatdev.rlmc.environment.Environment;

public interface PythonEntrypoint {
    void connectEnvironment(String type, Environment<?, ?> environment);
    void train(Environment<?, ?> environment);
}
