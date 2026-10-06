package com.h8.ems.simulator;

import com.h8.ems.simulator.config.ScenarioConfig;
import com.h8.ems.simulator.runner.ExperimentRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Simulator CLI entry point.
 * Usage: java -jar simulator.jar --mode=batch --scenarios=S1 --policies=B1,B2,P1 --reps=5 --out=experiments/results
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Map<String, String> params = parseArgs(args);

        String mode = params.getOrDefault("mode", "batch");
        List<String> scenarioNames = List.of(params.getOrDefault("scenarios", "S1").split(","));
        List<String> policyNames = List.of(params.getOrDefault("policies", "B1,B2,P1").split(","));
        int reps = Integer.parseInt(params.getOrDefault("reps", "3"));
        String outDir = params.getOrDefault("out", "experiments/results");
        long baseSeed = Long.parseLong(params.getOrDefault("seed", "42"));

        log.info("H8 EMS Simulator");
        log.info("  Mode: {}", mode);
        log.info("  Scenarios: {}", scenarioNames);
        log.info("  Policies: {}", policyNames);
        log.info("  Reps: {}", reps);
        log.info("  Output: {}", outDir);

        if ("replay".equals(mode)) {
            log.info("Replay mode is a stub — use batch mode for experiments.");
            return;
        }

        // Load scenarios
        List<ScenarioConfig> scenarios = new ArrayList<>();
        for (String name : scenarioNames) {
            scenarios.add(loadScenario(name));
        }

        // Generate seeds
        List<Long> seeds = new ArrayList<>();
        for (int i = 0; i < reps; i++) {
            seeds.add(baseSeed + i);
        }

        ExperimentRunner runner = new ExperimentRunner(scenarios, policyNames, seeds, Path.of(outDir));
        runner.run();

        log.info("All experiments complete. Results in {}", outDir);
    }

    private static ScenarioConfig loadScenario(String name) {
        // Try classpath first
        String resourcePath = "/scenarios/" + name + ".yaml";
        InputStream is = Main.class.getResourceAsStream(resourcePath);

        if (is == null) {
            // Try filesystem
            Path filePath = Path.of("experiments/scenarios", name + ".yaml");
            if (Files.exists(filePath)) {
                try {
                    is = Files.newInputStream(filePath);
                } catch (Exception e) {
                    log.warn("Cannot read scenario file {}: {}", filePath, e.getMessage());
                }
            }
        }

        if (is != null) {
            Yaml yaml = new Yaml();
            ScenarioConfig config = yaml.loadAs(is, ScenarioConfig.class);
            if (config.getName() == null || config.getName().equals("default")) {
                config.setName(name);
            }
            return config;
        }

        // Fall back to defaults
        log.info("No YAML found for scenario {}, using defaults", name);
        ScenarioConfig config = new ScenarioConfig();
        config.setName(name);
        return config;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                } else {
                    params.put(parts[0], "true");
                }
            }
        }
        return params;
    }
}
