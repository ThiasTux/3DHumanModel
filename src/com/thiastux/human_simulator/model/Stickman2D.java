package com.thiastux.human_simulator.model;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.HashMap;

public class Stickman2D extends Stickman{

    public Stickman2D(Node rootNode, HashMap<Integer, Spatial> map, AssetManager assetManager){
        this.skeletonMap = map;


    }

    @Override
    public void updateModelBonePosition(Quaternion animationQuaternion, int boneIndex) {
        Spatial bone = skeletonMap.get(boneIndex);
        bone.setLocalRotation(animationQuaternion);
    }

    @Override
    public void rotateLegs(Quaternion torsoQuaternion) {
        //Vector3f torsoVector3f = torsoQuaternion.getRotationColumn(2).normalizeLocal();
        //Quaternion pelvisQuaternion = new Quaternion().fromAngles(0, -torsoVector3f.angleBetween(Vector3f.UNIT_Z), 0);
        double heading;
        float qx = torsoQuaternion.getX();
        float qy = torsoQuaternion.getY();
        float qz = torsoQuaternion.getZ();
        float qw = torsoQuaternion.getW();
        double test = qx * qy + qz * qw;
        if (test > 0.400) { // singularity at north pole
            //heading = 2 * Math.atan2(qx, qw);
            //Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
            //pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
            return;
        }
        if (test < -0.400) { // singularity at south pole
            //heading = -2 * Math.atan2(qx, qw);
            //Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
            //pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
            return;
        }
        double sqx = qx * qx;
        double sqy = qy * qy;
        double sqz = qz * qz;
        heading = Math.atan2(2 * qy * qw - 2 * qx * qz, 1 - 2 * sqy - 2 * sqz);
        Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
        pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
    }

    @Override
    public void setShadowMode(RenderQueue.ShadowMode shadowMode) {
        for (Spatial bone : skeletonMap.values()) {
            bone.setShadowMode(shadowMode);
        }
    }
}
