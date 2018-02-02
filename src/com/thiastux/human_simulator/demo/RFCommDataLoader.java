package com.thiastux.human_simulator.demo;

import com.jme3.math.Quaternion;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class RFCommDataLoader extends Thread {

    private BufferedReader inputReader;
    private volatile Quaternion readQuat = new Quaternion();
    private boolean reading;
    private int[] columnsValue;
    private final Object lock;

    RFCommDataLoader(Object lock, String port, int[] columnsValue) {
        this.lock = lock;
        this.columnsValue = columnsValue;
        try {
            inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(port)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (reading) {
            try {
                String[] values = inputReader.readLine().split("\\s+");
                float qw = Float.parseFloat(values[columnsValue[0]]);
                float qx = Float.parseFloat(values[columnsValue[1]]);
                float qy = Float.parseFloat(values[columnsValue[2]]);
                float qz = Float.parseFloat(values[columnsValue[3]]);
                readQuat = new Quaternion(qx / 1000.0f, qy / 1000.0f, qz / 1000.0f, qw / 1000.0f);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {

            }
        }
    }

    public Quaternion getData() {
        return readQuat;
    }

    public void startExecution() {
        synchronized (lock) {
            reading = true;
        }
        this.start();
    }

    public void stopExecution() {
        synchronized (lock) {
            reading = false;
        }
        try {
            inputReader.close();
        } catch (IOException ex) {
            System.out.println("Closing socket error: " + ex.getMessage());
        } catch (NullPointerException ignored) {

        }
    }

}
