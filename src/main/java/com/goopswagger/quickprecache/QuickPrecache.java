package com.goopswagger.quickprecache;

import com.google.common.collect.Iterables;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

// this code kinda stinks... beware!!
public class QuickPrecache {
    public static int SPLIT_SIZE = 48;

    public static ArrayList<String> failedVpks = new ArrayList<>();
    public static HashSet<String> modelList = new HashSet<>();

    public static void main(String[] args) throws IOException, URISyntaxException {
        boolean auto = false;
        String scriptName = "";
        String path = new File(QuickPrecache.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-auto"))
                auto = true;
            if (args[i].equals("-list"))
                scriptName = args[i+1];
            if (args[i].equals("-chunksize"))
                SPLIT_SIZE = Integer.parseInt(args[i+1]);
        }

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

        int splitIndex = 0;
        for (List<String> split : Iterables.partition(modelList, SPLIT_SIZE)) {
            String splitFileName = "precache_" + splitIndex;
            File splitFile = new File(splitFileName + ".qc");
            splitFile.createNewFile();
            splitFile.deleteOnExit();
            BufferedWriter splitWriter = new BufferedWriter(new FileWriter(splitFile));
            splitWriter.write("$modelname \"" + splitFileName + ".mdl\"\n");
            for (String s : split) {
                splitWriter.write("$includemodel " + "\"" + s + "\"\n");
            }
            splitWriter.close();
            makeModel(path, "precache_" + splitIndex);
            splitIndex++;
        }

        File modelFile = new File("precache.qc");
        modelFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write("$modelname \"precache.mdl\"\n");
        for (int i = 0; i < splitIndex; i++) {
            writer.write("$includemodel " + "\"" + "precache_" + i + ".mdl\"\n");
        }
        writer.close();

        makeModel(path, "precache");

        if (!failedVpks.isEmpty()) {
            System.out.println("WARNING!!! Failed to load invalid vpk(s): ");
            for (String failedVpk : failedVpks) {
                System.out.println("\t" + failedVpk);
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

    public static void makeModel(String path, String file) throws IOException {
        String process = path + "/bin/studiomdl.exe\"";
        String pGame = "-game " +  "\"" + path + "/tf/\"";
        String pNop4 = "-nop4";
        String pVerbose = "-verbose";
        String pFile = file + ".qc";
        ProcessBuilder builder = new ProcessBuilder(process + " " + path + " " + pGame + " " + pNop4 + " " + pVerbose + " " + pFile);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }
}