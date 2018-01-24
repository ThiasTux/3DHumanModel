package com.thiastux.human_simulator;

import com.jme3.math.Quaternion;

/**
 * Created by ThiasTux.
 */
public class Utils {

    public static String quatToString(Quaternion q){
        return String.format("(w: %.4f, x: %.4f, y: %.4f, z: %.4f)", q.getW(), q.getX(), q.getY(), q.getZ());
    }

    public static String vectToString(Quaternion q){
        return String.format("(x: %.4f, y: %.4f, z: %.4f)", q.getX(), q.getY(), q.getZ());
    }
}
