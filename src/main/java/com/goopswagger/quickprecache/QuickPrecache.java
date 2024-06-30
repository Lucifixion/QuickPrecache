package com.goopswagger.quickprecache;

import java.io.*;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Scanner;

// this code kinda stinks... beware!!
public class QuickPrecache {
    public static final HashSet<String> modelList = new HashSet<>();

    public static void main(String[] args) throws IOException, URISyntaxException {
        File scriptFile = new File("precachelist.txt");
        if (!scriptFile.exists()) {
            if (scriptFile.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile));
                writer.write("///////////////////////////////////////////////////////////////////////////////////////////\n");
                writer.write("// models are included in a full path from the 'models' folder.\n");
                writer.write("// it is REQUIRED to run 'quickprecache.bat' after editing this script.\n");
                writer.write("// if you recieve the error \"Unsupported major.minor version\", you need a newer version of java\n");
                writer.write("// ( there is an open invitation for anyone to write a non-java app ( or just jpackage it, im too lazy for that ) )\n");
                writer.write("///////////////////////////////////////////////////////////////////////////////////////////\n");
                writer.write("// EXAMPLES BELOW VVVVV\n");
                writer.write("// weapons/c_models/c_grenadelauncher/c_grenadelauncher.mdl\n");
                writer.write("// weapons/c_models/c_scout_animations.mdl\n");
                writer.write("// player/scout.mdl\n");
                writer.write("///////////////////////////////////////////////////////////////////////////////////////////\n");
                writer.close();
            } else {
                System.out.println("FAILED TO CREATE FILE. SOMETHING IS WRONG.");
            }
            return;
        }
        Scanner myReader = new Scanner(scriptFile);
        System.out.println("Precached models:");
        while (myReader.hasNextLine()) {
            String baseModel = myReader.nextLine();
            String model = handleString(baseModel);

            if (baseModel.isEmpty())
                continue;

            if (baseModel.trim().startsWith("//"))
                continue;

            System.out.println("\t" + model);
            modelList.add(model);
        }
        myReader.close();

        File modelFile = new File("precache.qc");
        modelFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write("$modelname \"precache.mdl\"\n");
        for (String s : modelList) {
            writer.write("$includemodel " + "\"" + s + "\"\n");
        }
        writer.close();

        String path = new File(QuickPrecache.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();

        makeModel(path);
    }

    // correct any string issues
    public static String handleString(String input) {
        input = input.trim();
        if (input.startsWith("models/"))
            input = input.substring("models/".length());
        if (input.contains("//"))
            input = input.substring(0, input.indexOf("//")).trim();
        if (!input.endsWith(".mdl"))
            input += ".mdl";
        return input;
    }

    public static void makeModel(String path) throws IOException {
        String process = path + "/bin/studiomdl.exe\"";
        String pGame = "-game " +  "\"" + path + "/tf/\"";
        String pNop4 = "-nop4";
        String pVerbose = "-verbose";
        String pFile = "precache.qc";
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