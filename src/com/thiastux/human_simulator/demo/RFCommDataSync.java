package com.thiastux.human_simulator.demo;

import com.jme3.math.Quaternion;
import com.thiastux.human_simulator.model.Const;

public class RFCommDataSync extends Thread implements DataLoader {

    private final Object lock;
    private final Object[] childLocks;
    private RFCommDataLoader[] dataLoaders;
    private int[] animatedBones;
    private volatile Quaternion[] animationPacket = new Quaternion[12];
    private Quaternion[] tmpAnimationPacket = new Quaternion[12];
    private boolean isExecuted = false;

    RFCommDataSync(Object lock, String[] params) {
        this.lock = lock;
        int portsNum = params.length / 2;
        dataLoaders = new RFCommDataLoader[portsNum];
        childLocks = new Object[portsNum];
        animatedBones = new int[portsNum];
        for (int i = 0; i < portsNum; i++) {
            childLocks[i] = new Object();
            dataLoaders[i] = new RFCommDataLoader(childLocks, params[i * 2 + 1], new int[]{2, 3, 4, 5});
            try {
                String tmp = params[i * 2];
                animatedBones[i] = Const.BindColumIndex.get(tmp).getCode();
            } catch (NullPointerException ignored) {
            }
        }
    }

    @Override
    public void run() {
        while (isExecuted) {
            for (int i = 0; i < dataLoaders.length; i++) {
                tmpAnimationPacket[animatedBones[i]] = dataLoaders[i].getData();
            }
            Const.animationStart = true;
            animationPacket = tmpAnimationPacket;
        }
    }

    @Override
    public void stopExecution() {
        synchronized (lock) {
            isExecuted = false;
        }
        for (RFCommDataLoader dataLoader : dataLoaders) {
            try {
                dataLoader.stopExecution();
            } catch (NullPointerException ignored) {

            }
        }
    }

    @Override
    public Quaternion[] getData() {
        synchronized (lock) {
            return animationPacket;
        }
    }

    @Override
    public void startExecution() {
        synchronized (lock) {
            isExecuted = true;
            for (RFCommDataLoader dataLoader : dataLoaders) {
                dataLoader.startExecution();
            }
        }
        this.start();
    }
}
