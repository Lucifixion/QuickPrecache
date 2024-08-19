package com.goopswagger.quickprecache;

import lombok.SneakyThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class ConfigUtil {

    @SneakyThrows
    public static void checkRootLod(String path) {
        File configFile = new File(path + "/tf/cfg/config.cfg");
        if (configFile.exists()) {
            String configString = Files.readString(configFile.toPath());
            int rootLodIndex = configString.indexOf("r_rootlod");
            if (rootLodIndex > -1) {
                configString = configString.replace(configString.substring(rootLodIndex, configString.indexOf("\n", rootLodIndex)), "r_rootlod \"0\"");
                BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
                writer.write(configString);
                writer.close();
            }
        }
    }

}
