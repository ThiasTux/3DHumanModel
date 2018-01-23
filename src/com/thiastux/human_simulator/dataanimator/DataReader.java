package com.thiastux.human_simulator.dataanimator;

import com.jme3.math.Quaternion;
import com.sun.org.apache.xpath.internal.functions.WrongNumberArgsException;
import com.thiastux.human_simulator.Main;
import com.thiastux.human_simulator.demo.TCPDataClient;
import com.thiastux.human_simulator.model.Const;
import org.apache.commons.lang3.ObjectUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataReader {

    private Object lock;
    private HashMap<Integer, Float[]> columnIndexMap;
    private HashMap<Float, Quaternion[]> dataMap;
    private Quaternion[] priorQuaternions;
    private boolean isExecuted = false;
    private String inputPath;
    private BufferedReader dataLoader;


    public DataReader(Object lock, String[] args) {
        this.lock = lock;
        inputPath = args[0];
        parseParameters(Arrays.copyOfRange(args, 1, args.length - 1));
    }

    boolean loadData() {
        try {
            List<String> dataStringList = Files.readAllLines(Paths.get(inputPath+"joined_data.txt"));
            for (String line : dataStringList){
                String[] lineSplit = line.split(" ");
                String[] values = Arrays.copyOfRange(lineSplit, 1, lineSplit.length-1);
                Quaternion[] quaternions = new Quaternion[12];
                for(int i=0;i<12;i++){
                   try{
                       Float[] columnValues = columnIndexMap.get(i);

                   } catch (NullPointerException e){

                   }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
            columnIndexMap.put(i, new Float[4]);
        }
        try {
            //If the numbers of the parameters (-1 for the port number) isn't
            //multiple of 5, throw an exception
            if ((args.length - 2) % 5 != 0) {
                throw new WrongNumberArgsException("Wrong number of parameters!");
            }
            for (int i = 2; i < args.length; i += 5) {

                //Read the command
                String param = args[i];

                if (param.endsWith("l"))
                    Const.useLegs = true;

                //Get the index of the array corresponding to the command
                Float[] paramsValues = new Float[4];
                Integer limbColIndex = null;
                Integer limbPriorIndex = null;
                try {
                    limbColIndex = Const.BindColumIndex.get(param).getCode();
                } catch (NullPointerException e) {
                    limbPriorIndex = Const.PriorQuatIndex.get(param).getCode();
                }
                paramsValues[0] = Float.parseFloat(args[i + 1]);
                paramsValues[1] = Float.parseFloat(args[i + 2]);
                paramsValues[2] = Float.parseFloat(args[i + 3]);
                paramsValues[3] = Float.parseFloat(args[i + 4]);

                columnIndexMap.put(limbColIndex, paramsValues);
                if (limbPriorIndex != null) {
                    priorQuaternions[limbPriorIndex] = new Quaternion(paramsValues[0], paramsValues[1], paramsValues[2], paramsValues[3]);
                }
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        } catch (WrongNumberArgsException | NumberFormatException e) {
            Logger.getLogger(TCPDataClient.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }

    public Quaternion[] getData() {
        return new Quaternion[0];
    }
}
