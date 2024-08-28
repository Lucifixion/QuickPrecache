package com.goopswagger.quickprecache;

import com.google.common.collect.Iterables;
import lombok.SneakyThrows;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

// this code kinda stinks... beware!!
public class QuickPrecache {
    public static int splitSize = 48;

    public static ArrayList<String> failedVpks = new ArrayList<>();
    public static HashSet<String> modelList = new HashSet<>();

    public static boolean flush = false;
    public static boolean auto = false;
    public static boolean debug = false;

    public static int MAX_SPLIT_SIZE = 2048;

    @SneakyThrows
    public static void main(String[] args) throws IOException, URISyntaxException {
        String scriptName = "";
        String path = new File(QuickPrecache.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-auto" -> auto = true;
                case "-flush" -> flush = true;
                case "-debug" -> debug = true;
                case "-list" -> scriptName = args[i+1];
                case "-chunksize" -> System.out.println("!!! Chunksize parameter is deprecated !!!");
            }
        }

        StudioMDL.init(path);

        flushFiles(path);
        if (flush)
            return;

        ConfigUtil.checkRootLod(path);

        if (auto) {
            modelList = PrecacheListUtil.makePrecacheList();
            if (!scriptName.isEmpty()) {
                File file = new File(scriptName);
                file.createNewFile();
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
                for (String s : modelList) {
                    fileWriter.write(s + "\n");
                }
                fileWriter.close();
            }
        }
        else {
            modelList = new HashSet<>();
            if (scriptName.isEmpty())
                scriptName = "precachelist.txt";
            Scanner scriptReader = new Scanner(new File(scriptName));
            while (scriptReader.hasNextLine()) {
                String baseModel = scriptReader.nextLine();
                String model = handleString(baseModel);

                if (baseModel.isEmpty())
                    continue;

                if (baseModel.trim().startsWith("//"))
                    continue;

                if (modelList.add(model))
                    System.out.println(model);
            }
            scriptReader.close();
        }

        System.out.println("Precached models:");
        for (String s : modelList)
            System.out.println(s);

        makePrecacheSubList(path, modelList);
        makePrecacheListFile(path);

//        int splitIndex = 0;
//        for (List<String> split : Iterables.partition(modelList, splitSize)) {
//            String splitFileName = "precache_" + splitIndex;
//            File splitFile = new File(splitFileName + ".qc");
//            splitFile.createNewFile();
//            if (!debug)
//                splitFile.deleteOnExit();
//            BufferedWriter splitWriter = new BufferedWriter(new FileWriter(splitFile));
//            splitWriter.write(\n");
//            for (String s : split) {
//                splitWriter.write("$includemodel " + "\"" + s + "\"\n");
//            }
//            splitWriter.close();
//            splitIndex++;
//        }

//        File modelFile = new File("precache.qc");
//        if (!debug)
//            modelFile.deleteOnExit();
//        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
//        writer.write("$modelname \"precache.mdl\"\n");
//        for (int i = 0; i < splitIndex; i++) {
//            writer.write("$includemodel " + "\"" + "precache_" + i + ".mdl\"\n");
//        }
//        writer.close();
//
//        Thread.sleep(500); // ???????
//
//        StudioMDL.makeModel(path, "precache");
//
//        for (int i = 0; i < splitIndex; i++) {
//            StudioMDL.makeModel(path, "precache_" + i);
//        }
//
//        if (!failedVpks.isEmpty()) {
//            System.out.println("WARNING!!! Failed to load invalid vpk(s): ");
//            for (String failedVpk : failedVpks) {
//                System.out.println("\t" + failedVpk);
//            }
//        }
    }

    public static String getModelName(String model) {
        return "$modelname \"" + model + ".mdl\"\n";
    }

    public static String getIncludeModel(String include) {
        return "$includemodel " + "\"" + include + "\"\n";
    }

    static int builderIndex = 0;

    public static void makePrecacheSubList(String path, Set<String> strings) {
        StringBuilder builder = getPrecacheStringBuilder(builderIndex);
        Set<String> passedStrings = new HashSet<>();
        for (String s : Iterables.cycle(strings)) {
            if( passedStrings.contains(s) )
                continue;
            if( !((builder.length() + getIncludeModel(s).length()) > MAX_SPLIT_SIZE) ) {
                builder.append(getIncludeModel(s));
                passedStrings.add(s);
            } else {
                makePrecacheSubListFile(path, "precache_" + builderIndex + ".qc", builder.toString());
                builder = getPrecacheStringBuilder(++builderIndex);
            }
            if (strings.size() == passedStrings.size()) {
                makePrecacheSubListFile(path, "precache_" + builderIndex + ".qc", builder.toString());
                ++builderIndex;
                break;
            }

        }
    }

    @SneakyThrows
    public static void makePrecacheSubListFile(String path, String filename, String data) {
        File modelFile = new File(filename);
        if (!debug)
            modelFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write(data);
        writer.close();
        StudioMDL.makeModel(path, filename);
    }

    @SneakyThrows
    public static void makePrecacheListFile(String path) {
        File modelFile = new File("precache.qc");
        if (!debug)
            modelFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write(getModelName("precache"));
        for (int i = 0; i < builderIndex; i++) {
            writer.write(getIncludeModel("precache_" + i + ".mdl"));
        }
        writer.close();
        StudioMDL.makeModel(path, "precache.qc");
    }

    public static StringBuilder getPrecacheStringBuilder(int index) {
        StringBuilder newStringBuilder = new StringBuilder();
        newStringBuilder.append(getModelName("precache_" + index));
        return newStringBuilder;
    }

    @SneakyThrows
    public static void flushFiles(String path) {
        File modelsFolder = Path.of(path + "/tf/models").toFile();
        if (modelsFolder.exists()) {
            for (File file : modelsFolder.listFiles()) {
                if (file.getName().equals("precache.mdl") || (file.getName().startsWith("precache_") && file.getName().endsWith(".mdl")))
                    file.delete();
            }
        }
    }

    // correct any string issues
    public static String handleString(String input) {
        input = input.trim();
        input = input.replaceAll("\"", "");
        if (input.startsWith("models/"))
            input = input.substring("models/".length());
        if (input.contains("//"))
            input = input.substring(0, input.indexOf("//")).trim();
        if (!input.endsWith(".mdl"))
            input += ".mdl";
        return input;
    }
}