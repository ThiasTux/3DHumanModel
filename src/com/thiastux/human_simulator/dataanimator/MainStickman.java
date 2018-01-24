/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thiastux.human_simulator.dataanimator;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.thiastux.human_simulator.Utils;
import com.thiastux.human_simulator.model.Const;
import com.thiastux.human_simulator.model.Stickman;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * @author mathias
 */
public class MainStickman extends SimpleApplication implements ScreenController {

    private static boolean DEBUG = false;
    Element playedPanel;
    Element remainingPanel;
    Element playPanel;
    int maxWidth;

    private DataReader dataReader;
    private Quaternion[] animationQuaternions;
    private HashMap<Integer, Spatial> skeletonMap = new HashMap<>();
    private Stickman stickman;
    private Geometry terrainGeometry;

    private final float TERRAIN_WIDTH = 50f;
    private final float TERRAIN_HEIGHT = 50f;
    private Quaternion[] previousQuaternions = new Quaternion[12];
    private Quaternion preRot;
    private Quaternion qAlignArmR;
    private Quaternion qAlignArmL;
    float percPlay;
    //Running params
    boolean isRunning = false;
    long applicationStartTime;
    long applicationEndTime;
    Float startTime = 0f;
    Float currentTime = 0f;
    double animationSpeed = 1;
    int speedIndex = 1;
    double[] speedFactors = {0.5, 1, 2, 4, 8};
    private TreeMap<Float, Quaternion[]> dataMap;
    private int samplingFreq = 500;
    private float elapsedTime = 0.0f;
    //Gui and controls
    private Nifty nifty;
    private int animationIndex;
    private int dataKeysetSize;
    private MainStickman() {
        super();
    }

    private MainStickman(String[] args) {
        dataReader = new DataReader(args);
        dataMap = dataReader.loadData();
        dataKeysetSize = dataMap.keySet().size();
    }

    public static void main(String[] args) {

        MainStickman app;

        if (DEBUG) {
            app = new MainStickman();
        } else {
            app = new MainStickman(args);
        }

        AppSettings appSettings = new AppSettings(true);
        appSettings.setFrameRate(60);
        appSettings.setResolution(1280,800);

        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setSettings(appSettings);

        app.start();

    }

    @Override
    public void simpleInitApp() {
        System.out.println("Application initialization started");

        //addReferenceSystem();

        flyCam.setEnabled(false);
        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setDefaultHorizontalRotation((float) Math.toRadians(90));
        chaseCam.setDefaultVerticalRotation((float) Math.toRadians(30 / 2));
        chaseCam.setDefaultDistance(40f);

        setDisplayFps(true);

        setDisplayStatView(false);

        setPauseOnLostFocus(false);

        createHumanModel();

        loadTerrain();

        setSunLightAndShadow();

        loadInterface();

        computeInitialQuaternions();

        isRunning = false;

    }

    @Override
    public void simpleUpdate(float tpf) {
        if (animationIndex == 0)
            startTime = dataMap.firstKey();
        if (isRunning) {
            elapsedTime += (float) (animationSpeed * tpf);
            if (elapsedTime >= (1 / samplingFreq))
                animateModel(elapsedTime);
            elapsedTime = .0f;
        }
    }

    @Override
    public void stop() {
        System.out.println("\nApplication ended");
        super.stop();
    }

    private void animateModel(float tmpElapsedTime) {

        if (animationIndex < dataKeysetSize - 1) {

            currentTime = startTime + tmpElapsedTime;

            float lastKey = dataMap.subMap(startTime, currentTime).lastKey();
            if (lastKey != startTime) {

                int size = dataMap.subMap(startTime, currentTime).size();
                animationIndex += size;

                int perc = maxWidth * animationIndex / dataKeysetSize;
                System.out.println(perc);
                playedPanel.setWidth(perc);

                animationQuaternions = dataMap.get(lastKey);
                updateModel();
                startTime = lastKey;
            }

        } else {
            animationIndex = 0;
            elapsedTime = .0f;
            play();
            playedPanel.setWidth(0);
            stickman.resetPositions();
        }
    }


    private void updateModel() {
        for (int i = 0; i < 12; i++) {
            Quaternion rotQuat = preProcessingQuaternion(i);
            if (rotQuat != null) {
                stickman.updateModelBonePosition(rotQuat, i);
            }
        }
        if (!Const.useLegs) {
            stickman.rotateLegs(previousQuaternions[0]);
        }
    }

    private Quaternion preProcessingQuaternion(int i) {

        if (animationQuaternions[i] == null) {
            return null;
        }

        //Normalize quaternion to adjust lost of precision using mG.
        Quaternion outputQuat = animationQuaternions[i].normalizeLocal();

        outputQuat = outputQuat.mult(preRot);
        outputQuat = new Quaternion(-outputQuat.getX(),
                outputQuat.getZ(),
                outputQuat.getY(),
                outputQuat.getW());

        previousQuaternions[i] = outputQuat.normalizeLocal();

        outputQuat = conjugate(getPrevLimbQuaternion(i)).mult(outputQuat);

        outputQuat = outputQuat.normalizeLocal();

        return outputQuat;
    }

    private void loadInterface() {
        //Load and add GUI as overlay on the scene
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                viewPort);

        nifty = niftyDisplay.getNifty();
        nifty.fromXml("interfaces/hud/controlsLayout.xml", "controls", this);
        guiViewPort.addProcessor(niftyDisplay);

        //Setup playbar to increase width depending on time of the animation
        playedPanel = nifty.getCurrentScreen().findElementById("playedBar");
        playPanel = nifty.getCurrentScreen().findElementById("playBar");
        maxWidth = playPanel.getWidth();
        System.out.println("maxWidth: " + maxWidth);
    }

    private void computeInitialQuaternions() {
        //Prerotation quaternion
        preRot = new Quaternion().fromAngles((float) Math.toRadians(90), 0f, 0f);

        System.out.println("preRot" + Utils.quatToString(preRot));

        for (int i = 0; i < 12; i++) {
            previousQuaternions[i] = new Quaternion();
        }

    }

    private Quaternion conjugate(Quaternion quaternion) {
        return new Quaternion(-quaternion.getX(), -quaternion.getY(), -quaternion.getZ(), quaternion.getW());
    }

    private Quaternion getPrevLimbQuaternion(int i) {
        switch (i) {
            case 1:
            case 3:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                return previousQuaternions[i - 1];
            case 2:
            case 5:
            case 8:
            case 10:
                return previousQuaternions[0];
            default:
                return Quaternion.IDENTITY;
        }

    }

    private void addReferenceSystem() {

        Node refNode = new Node("RefNode");

        Line xAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(3, 0, 0));
        Geometry xAxisGeometry = new Geometry("xAxis", xAxisline);
        Material xLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        xLineMaterial.getAdditionalRenderState().setLineWidth(2);
        xLineMaterial.setColor("Color", ColorRGBA.Green);
        xAxisGeometry.setMaterial(xLineMaterial);

        Line yAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0));
        Geometry yAxisGeometry = new Geometry("yAxis", yAxisline);
        Material yLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        yLineMaterial.getAdditionalRenderState().setLineWidth(2);
        yLineMaterial.setColor("Color", ColorRGBA.Blue);
        yAxisGeometry.setMaterial(yLineMaterial);

        Line zAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 0, 3));
        Geometry zAxisGeometry = new Geometry("zAxis", zAxisline);
        Material zLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        zLineMaterial.getAdditionalRenderState().setLineWidth(2);
        zLineMaterial.setColor("Color", ColorRGBA.Red);
        zAxisGeometry.setMaterial(zLineMaterial);

        refNode.attachChild(xAxisGeometry);
        refNode.attachChild(yAxisGeometry);
        refNode.attachChild(zAxisGeometry);

        refNode.setLocalTranslation(10, 0, 0);

        rootNode.attachChild(refNode);
    }

    private void createHumanModel() {
        stickman = new Stickman(rootNode, skeletonMap, assetManager);
    }

    private void loadTerrain() {
        Quad terrainMesh = new Quad(TERRAIN_WIDTH, TERRAIN_HEIGHT);
        terrainGeometry = new Geometry("Terrain", terrainMesh);
        terrainGeometry.setLocalRotation(new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f));
        terrainGeometry.setLocalTranslation(-TERRAIN_WIDTH / 2, -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH), TERRAIN_HEIGHT / 2);
        Material terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        terrainMaterial.setBoolean("UseMaterialColors", true);
        terrainMaterial.setColor("Ambient", ColorRGBA.White);
        terrainMaterial.setColor("Diffuse", ColorRGBA.White);
        terrainGeometry.setMaterial(terrainMaterial);

        terrainGeometry.setShadowMode(ShadowMode.Receive);

        rootNode.attachChild(terrainGeometry);


    }

    private void setSunLightAndShadow() {
        //Add light to the scene
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White);
        sun2.setDirection(new Vector3f(-.5f, .5f, .5f).normalizeLocal());
        rootNode.addLight(sun2);

        rootNode.setShadowMode(ShadowMode.Off);

        stickman.setShadowMode(ShadowMode.CastAndReceive);
        terrainGeometry.setShadowMode(ShadowMode.Receive);

        final int SHADOWMAP_SIZE = 512;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);

        viewPort.setBackgroundColor(ColorRGBA.White);

    }

    private void setAmbientLightAndShadow() {
        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(light);

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);

        final int SHADOWMAP_SIZE = 512;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);
    }

    private void setSunLightsAndShadows() {

        //Add light to the scene
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White);
        sun2.setDirection(new Vector3f(.5f, .5f, .5f).normalizeLocal());
        rootNode.addLight(sun2);

        rootNode.setShadowMode(ShadowMode.Off);

        stickman.setShadowMode(ShadowMode.CastAndReceive);
        terrainGeometry.setShadowMode(ShadowMode.Receive);

        final int SHADOWMAP_SIZE = 512;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);

        DirectionalLightShadowRenderer dlsr2 = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr2.setLight(sun2);
        viewPort.addProcessor(dlsr2);

        DirectionalLightShadowFilter dlsf2 = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf2.setLight(sun2);
        dlsf2.setEnabled(true);
        FilterPostProcessor fpp2 = new FilterPostProcessor(assetManager);
        fpp2.addFilter(dlsf2);
        viewPort.addProcessor(fpp2);

    }

    //Executed on click of the Play/Pause button
    public void play() {
        if (isRunning) {
            isRunning = false;
            NiftyImage image = nifty.getRenderEngine().createImage(nifty.getCurrentScreen(), "interfaces/hud/play-button.png", false);
            Element niftyElement = nifty.getCurrentScreen().findElementByName("button");
            niftyElement.getRenderer(ImageRenderer.class).setImage(image);
            applicationEndTime = System.currentTimeMillis();
            System.out.println("Pause - Time: " + applicationEndTime);
            System.out.println("T exec: " + (applicationEndTime - applicationStartTime));
            //headBone.setUserControl(paused);
        } else {
            isRunning = true;
            NiftyImage image = nifty.getRenderEngine().createImage(nifty.getCurrentScreen(), "interfaces/hud/pause-button.png", false);
            Element niftyElement = nifty.getCurrentScreen().findElementByName("button");
            niftyElement.getRenderer(ImageRenderer.class).setImage(image);
            applicationStartTime = System.currentTimeMillis();

            System.out.println("Play - Time: " + applicationStartTime);
        }
    }

    //Executed on click on the play bar
    public void clickBar(int x, int y) {
        int panelPosX = playPanel.getX();
        int relX = x - panelPosX;
        int xI = dataKeysetSize * relX / maxWidth;
        startTime = (Float) dataMap.keySet().toArray()[xI];
        animationIndex = xI;
        System.out.println("starttime: " + startTime);
    }

    //Executed on scale speed button
    public void scaleSpeed() {
        speedIndex = (speedIndex + 1) % speedFactors.length;
        animationSpeed = speedFactors[speedIndex];
        Element niftyElement = nifty.getCurrentScreen().findElementById("speedText");
        niftyElement.getRenderer(TextRenderer.class).setText("x" + animationSpeed);
        System.out.println("Speed: " + animationSpeed);
    }


    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {

    }

    @Override
    public void onStartScreen() {

    }

    @Override
    public void onEndScreen() {

    }
}
