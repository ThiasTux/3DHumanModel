package com.thiastux.human_simulator.demo;

import com.jme3.math.Quaternion;

public interface DataLoader {
    void stopExecution();

    Quaternion[] getData();

    void startExecution();
}
