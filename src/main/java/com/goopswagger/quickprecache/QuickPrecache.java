package com.goopswagger.quickprecache;

import com.google.common.collect.Iterables;
import lombok.SneakyThrows;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

// this code kinda stinks... beware!!
public class QuickPrecache {
    public static StudioMdlVersion studioMdlVersion = StudioMdlVersion.MISSING;
    public static int splitSize = 48;

    public static ArrayList<String> failedVpks = new ArrayList<>();
    public static HashSet<String> modelList = new HashSet<>();

    public static boolean flush = false;
    public static boolean auto = false;
    public static boolean debug = false;

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
                case "-chunksize" -> splitSize = Integer.parseInt(args[i+1]);
            }
        }

        studioMdlVersion = getStudioMdlVersion(path);

        if (studioMdlVersion == StudioMdlVersion.MISSING) {
            System.out.println("StudioMDL.exe not found, you probably installed the mod wrong.");
            System.out.println("!!! QuickPrecache does not support linux.");
            return;
        }

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

        Thread.sleep(500); // ???????

        int splitIndex = 0;
        for (List<String> split : Iterables.partition(modelList, splitSize)) {
            String splitFileName = "precache_" + splitIndex;
            File splitFile = new File(splitFileName + ".qc");
            splitFile.createNewFile();
            if (!debug)
                splitFile.deleteOnExit();
            BufferedWriter splitWriter = new BufferedWriter(new FileWriter(splitFile));
            splitWriter.write("$modelname \"" + splitFileName + ".mdl\"\n");
            for (String s : split) {
                splitWriter.write("$includemodel " + "\"" + s + "\"\n");
            }
            splitWriter.close();
            splitIndex++;
        }

        File modelFile = new File("precache.qc");
        if (!debug)
            modelFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write("$modelname \"precache.mdl\"\n");
        for (int i = 0; i < splitIndex; i++) {
            writer.write("$includemodel " + "\"" + "precache_" + i + ".mdl\"\n");
        }
        writer.close();

        Thread.sleep(500); // ???????

        makeModel(path, "precache");

        for (int i = 0; i < splitIndex; i++) {
            makeModel(path, "precache_" + i);
        }

        if (!failedVpks.isEmpty()) {
            System.out.println("WARNING!!! Failed to load invalid vpk(s): ");
            for (String failedVpk : failedVpks) {
                System.out.println("\t" + failedVpk);
            }
        }
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

    public static void makeModel(String path, String file) throws IOException {
        String process = path + "/" + studioMdlVersion.path + "\"";
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

    public static StudioMdlVersion getStudioMdlVersion(String path) {
        if (checkStudioMdlVersion(path, StudioMdlVersion.NEKOMDL))
            return StudioMdlVersion.NEKOMDL;
//        if (checkStudioMdlVersion(path, StudioMdlVersion.STUDIOMDL64))
//            return StudioMdlVersion.STUDIOMDL64;
        if (checkStudioMdlVersion(path, StudioMdlVersion.STUDIOMDL32))
            return StudioMdlVersion.STUDIOMDL32;
        return StudioMdlVersion.MISSING;
    }

    public static boolean checkStudioMdlVersion(String path, StudioMdlVersion studioMdlVersion) {
        File studioMdlFile = new File(path + "/" + studioMdlVersion.path);
        if (studioMdlFile.exists()) {
            System.out.println(studioMdlVersion.path + " found.");
            return true;
        }
        return false;
    }

    public enum StudioMdlVersion {
        MISSING(""),
//        STUDIOMDL64("bin/x64/studiomdl.exe"), // studiomdl x64 doesn't work
        STUDIOMDL32("bin/studiomdl.exe"),
        NEKOMDL("bin/nekomdl.exe");

        public final String path;

        StudioMdlVersion(String path) {
            this.path = path;
        }
    }
}