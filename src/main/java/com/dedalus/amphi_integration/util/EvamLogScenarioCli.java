package com.dedalus.amphi_integration.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Path;

public final class EvamLogScenarioCli {

    private EvamLogScenarioCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: EvamLogScenarioCli <logPath> [outputPath]");
        }

        Path logPath = Path.of(args[0]);
        Path outputPath = args.length == 2
                ? Path.of(args[1])
                : logPath.resolveSibling(logPath.getFileName().toString() + ".scenario.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        EvamLogScenarioExtractor extractor = new EvamLogScenarioExtractor(gson);
        extractor.writeScenario(logPath, outputPath);
    }
}