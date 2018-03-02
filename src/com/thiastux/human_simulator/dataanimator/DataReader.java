package com.thiastux.human_simulator.dataanimator;

import com.jme3.math.Quaternion;
import com.sun.org.apache.xpath.internal.functions.WrongNumberArgsException;
import com.thiastux.human_simulator.Main;
import com.thiastux.human_simulator.model.Const;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataReader {

    private HashMap<Integer, Integer[]> columnIndexMap;
    private TreeMap<Double, Quaternion[]> dataMap;
    private Quaternion[] priorQuaternions;
    private boolean isExecuted = false;
    private String inputPath;
    private BufferedReader dataLoader;


    public DataReader(String[] args) {
        inputPath = args[0];
        parseParameters(Arrays.copyOfRange(args, 1, args.length));
        dataMap = new TreeMap<>();
    }

    TreeMap<Double, Quaternion[]> loadData() {

        float qw;
        float qx;
        float qy;
        float qz;
        double time;

        try {
            List<String> dataStringList = Files.readAllLines(Paths.get(inputPath));
            for (String line : dataStringList) {
                String[] lineSplit = line.split(" ");
                Quaternion[] quaternions = new Quaternion[12];
                time = Double.parseDouble(lineSplit[0]);
                for (int i = 0; i < 12; i++) {
                    try {
                        Integer[] columnValues = columnIndexMap.get(i);
                        qw = Float.parseFloat(lineSplit[columnValues[0]]);
                        qx = Float.parseFloat(lineSplit[columnValues[1]]);
                        qy = Float.parseFloat(lineSplit[columnValues[2]]);
                        qz = Float.parseFloat(lineSplit[columnValues[3]]);
                        quaternions[i] = new Quaternion(qx, qy, qz, qw);
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                        quaternions[i] = null;
                    }
                }
                dataMap.put(time, quaternions);
            }
            return dataMap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse parameters to bind column of the dataset with the quaternion of the
     * correct limb.
     *
     * @param args
     */
    private void parseParameters(String[] args) {
        columnIndexMap = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            columnIndexMap.put(i, new Integer[4]);
        }
        try {
            //If the numbers of the parameters (-1 for the port number) isn't
            //multiple of 5, throw an exception
            if ((args.length) % 5 != 0) {
                throw new WrongNumberArgsException("Wrong number of parameters!");
            }
            for (int i = 0; i < args.length; i += 5) {

                //Read the command
                String param = args[i];

                if (param.endsWith("l"))
                    Const.useLegs = true;

                //Get the index of the array corresponding to the command
                Integer[] paramsValues = new Integer[4];
                Integer limbColIndex = null;
                try {
                    limbColIndex = Const.BindColumIndex.get(param).getCode();
                } catch (NullPointerException ignored) {

                }
                paramsValues[0] = Integer.parseInt(args[i + 1]);
                paramsValues[1] = Integer.parseInt(args[i + 2]);
                paramsValues[2] = Integer.parseInt(args[i + 3]);
                paramsValues[3] = Integer.parseInt(args[i + 4]);

                columnIndexMap.put(limbColIndex, paramsValues);
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        } catch (WrongNumberArgsException | NumberFormatException e) {
            Logger.getLogger(DataReader.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }

}
