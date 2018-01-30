package com.thiastux.human_simulator.demo;

import com.jme3.math.Quaternion;
import com.thiastux.human_simulator.model.Const;

import java.io.*;

public class RFCommDataLoader extends Thread implements DataLoader {

    private final Object lock;
    private String[] ports;
    private BufferedReader[] inputReaders = new BufferedReader[12];
    private Quaternion[] animationPacket;
    private boolean isExecuted = false;

    RFCommDataLoader(Object lock, String[] ports) {
        this.lock = lock;
        this.ports = ports;
        animationPacket = new Quaternion[12];
        for (int i = 0; i < ports.length; i++) {
            try {
                inputReaders[i] = new BufferedReader(new InputStreamReader(new FileInputStream(ports[i])));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        String[] lines = new String[inputReaders.length];
        Quaternion[] tmpQuaternion = new Quaternion[12];

        while (isExecuted) {
            try {
                for (int i = 0; i < inputReaders.length; i++) {
                    if (inputReaders[i]!=null)
                        lines[i] = inputReaders[i].readLine();
                }
                for (int i = 0; i < lines.length; i++) {
                    String[] values = lines[i].split("\\s+");
                    int len = values.length;
                    float qw = Float.parseFloat(values[len - 4]);
                    float qx = Float.parseFloat(values[len - 3]);
                    float qy = Float.parseFloat(values[len - 2]);
                    float qz = Float.parseFloat(values[len - 1]);
                    tmpQuaternion[i] = new Quaternion(qx / 1000.0f, qy / 1000.0f, qz / 1000.0f, qw / 1000.0f);
                    synchronized (lock) {
                        Const.animationStart = true;
                        animationPacket = tmpQuaternion;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopExecution() {
        synchronized (lock) {
            isExecuted = false;
        }
        for (BufferedReader inputReader : inputReaders) {
            try {
                inputReader.close();
            } catch (IOException ex) {
                System.out.println("Closing socket error: " + ex.getMessage());
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
        }
        this.start();
    }
}
