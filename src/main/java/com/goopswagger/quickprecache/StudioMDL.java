package com.goopswagger.quickprecache;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class StudioMDL {
    public static StudioMDL.Version studioMdlVersion = StudioMDL.Version.MISSING;
    public static void init(String path) {
        studioMdlVersion = getStudioMdlVersion(path);

        if (studioMdlVersion == Version.MISSING) {
            System.out.println("StudioMDL.exe not found, you probably installed the mod wrong.");
            System.out.println("!!! IF YOU ARE ON LINUX, YOU NEED THE BIN/ FOLDER FROM A WINDOWS VERSION OF THE GAME !!!");
            throw new RuntimeException();
        }
    }

    public static void makeModel(String path, String file) throws IOException {
        String process = path + "/" + studioMdlVersion.path + "\"";
        String pGame = "-game " +  "\"" + path + "/tf/\"";
        String pNop4 = "-nop4";
        String pVerbose = "-verbose";
        String pFile = file;
        Process p = createProcessCrossPlatform(process + " " + path + " " + pGame + " " + pNop4 + " " + pVerbose + " " + pFile).start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }

    public static Version getStudioMdlVersion(String path) {
        if (checkStudioMdlVersion(path, Version.NEKOMDL))
            return Version.NEKOMDL;
//        if (checkStudioMdlVersion(path, StudioMdlVersion.STUDIOMDL64))
//            return StudioMdlVersion.STUDIOMDL64;
        if (checkStudioMdlVersion(path, Version.STUDIOMDL32))
            return Version.STUDIOMDL32;
        return Version.MISSING;
    }

    private static boolean checkStudioMdlVersion(String path, Version studioMdlVersion) {
        File studioMdlFile = new File(path + "/" + studioMdlVersion.path);
        if (studioMdlFile.exists()) {
            System.out.println(studioMdlVersion.path + " found.");
            return true;
        }
        return false;
    }

    public enum Version {
        MISSING(""),
        //        STUDIOMDL64("bin/x64/studiomdl.exe"), // studiomdl x64 doesn't work
        STUDIOMDL32("bin/studiomdl.exe"),
        NEKOMDL("bin/nekomdl.exe");

        public final String path;

        Version(String path) {
            this.path = path;
        }
    }

    public static ProcessBuilder createProcessCrossPlatform(String path) throws IOException {
        ProcessBuilder pb;
        if (!System.getProperty("os.name").contains("Windows")) { // UNIX - run all exes through WINE
            String[] pcmd = new String[] {"bash", "-c", "wine" + " \"" + path};
            pb = new ProcessBuilder(pcmd);
            pb.redirectErrorStream(true);

            // debug
            String s = "";
            for (String st : pcmd) {
                s = s + " " + st;
            }
            System.out.println(s);

            return pb;
        } else { // WinNT - native
            pb = new ProcessBuilder(path);
            pb.redirectErrorStream(true);
            return pb;
        }
    }
}
