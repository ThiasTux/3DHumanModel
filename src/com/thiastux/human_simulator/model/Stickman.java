package com.thiastux.human_simulator.model;

import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;

import java.util.HashMap;

public abstract class Stickman {
    public final float TORSO_HEIGHT = 4f;
    final float TORSO_RADIUS = 1.5f;
    final float PELVIS_HEIGHT = 2f;
    final float PELVIS_RADIUS = 1.0f;
    public final float HIP_WIDTH = 2f;
    public final float ULEG_LENGTH = 4f;
    public final float LLEG_LENGTH = 3f;
    final float ULEG_RADIUS = 0.5f;
    final float LLEG_RADIUS = 0.4f;
    final float UARM_LENGTH = 3f;
    final float LARM_LENGTH = 3f;
    final float UARM_RADIUS = 0.4f;
    final float LARM_RADIUS = 0.35f;
    final float HAND_WIDTH = 0.4f;
    final float HAND_LENGTH = 0.6f;
    final float HAND_THICKNESS = 0.1f;
    public final float SHOULDER_RADIUS = 6.5f;
    final float HEAD_RADIUS = 1f;
    final float EYE_RADIUS = 0.08f;
    public final float PUPIL_RADIUS = 0.2f;
    public final float SHOULDER_WIDTH = TORSO_RADIUS + UARM_RADIUS;

    HashMap<Integer, Spatial> skeletonMap;
    Spatial pelvisBone;

    public abstract void updateModelBonePosition(Quaternion rotQuat, int i);
    public abstract void rotateLegs(Quaternion quaternion);
    public abstract void setShadowMode(RenderQueue.ShadowMode shadowMode);
}
