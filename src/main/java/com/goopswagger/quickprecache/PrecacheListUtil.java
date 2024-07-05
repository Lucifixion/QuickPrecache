package com.goopswagger.quickprecache;

import com.connorhaigh.javavpk.core.Archive;
import com.connorhaigh.javavpk.core.Directory;
import com.connorhaigh.javavpk.core.Entry;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class PrecacheListUtil {

    @SneakyThrows
    public static HashSet<String> makePrecacheList() {
        HashSet<String> modelList = new HashSet<>();
        String path = new File(PrecacheListUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        File customFolder = new File(path + "/tf/custom");
        if (customFolder.isDirectory()) {
            List<String> list = new ArrayList<>();
            for (File file : customFolder.listFiles()) {
                if (file.isDirectory() && !file.getName().contains("disabled"))
                    list.addAll(manageFolder(file));
                else if (file.isFile() && file.getName().endsWith(".vpk"))
                    list.addAll(manageVPK(file));
            }
            for (String s : list) {
                if (!s.contains("decompiled ") && !s.contains("competitive_badge.mdl") && !s.contains("c_models/animations/"))
                    modelList.add(s);
            }
        }
        return modelList;
    }

    @SneakyThrows
    public static List<String> manageFolder(File file) {
        List<String> list = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(file.toPath())) {
            stream.filter(Files::isRegularFile)
                    .forEach(str -> {
                        String entry = str.toString().substring(file.getAbsolutePath().length() + 1);
                        if (entry.endsWith(".mdl"))
                            list.add(entry.replace("\\", "/"));
                    });
        }

        return list;
    }

    @SneakyThrows
    public static List<String> manageVPK(File file) {
        List<String> list = new ArrayList<>();
        Archive archive = new Archive(file);

        if (archive.load()) {
            for (Directory directory : archive.getDirectories()) {
                for (Entry entry : directory.getEntries()) {
                    String entryPath = directory.getPath();
                    String entryName = entry.getFullName();
                    if (entryPath.startsWith("models/") && entryName.endsWith(".mdl")) {
                        list.add(entryPath.substring("models/".length()) + "/" + entryName);
                    }
                }
            }
        } else {
            QuickPrecache.failedVpks.add(file.getAbsolutePath());
        }

        return list;
    }
}