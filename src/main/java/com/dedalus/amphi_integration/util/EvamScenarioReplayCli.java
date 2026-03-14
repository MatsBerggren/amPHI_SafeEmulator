package com.dedalus.amphi_integration.util;

import com.dedalus.amphi_integration.EvamIntegrationService;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayResult;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class EvamScenarioReplayCli {

    private EvamScenarioReplayCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: EvamScenarioReplayCli <scenarioPath> [summaryPath]");
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Path scenarioPath = Path.of(args[0]);
        Path summaryPath = args.length == 2
                ? Path.of(args[1])
                : scenarioPath.resolveSibling(scenarioPath.getFileName().toString() + ".replay-summary.json");

        EvamLogScenario scenario = gson.fromJson(Files.readString(scenarioPath), EvamLogScenario.class);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EvamIntegrationService.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run()) {
            EvamScenarioReplayService replayService = context.getBean(EvamScenarioReplayService.class);
            EvamScenarioReplayResult result = replayService.replay(scenario);
            Files.writeString(summaryPath, gson.toJson(result));
        }
    }
}