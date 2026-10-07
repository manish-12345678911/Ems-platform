package com.h8.ems.simulator.runner;

import com.h8.ems.simulator.config.ScenarioConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentRunnerTest {

    @Test
    void runProducesCsvWithHeaderAndDataRows(@TempDir Path tempDir) throws IOException {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("test");
        config.setDurationHours(8);
        config.setBaseDemandPerHour(5.0);

        ExperimentRunner runner = new ExperimentRunner(
                List.of(config),
                List.of("B1"),
                List.of(42L),
                tempDir
        );
        runner.run();

        Path csvFile = tempDir.resolve("test_results.csv");
        assertTrue(Files.exists(csvFile), "CSV file should be created");

        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.size() >= 2, "Should have header + at least 1 data row, got " + lines.size());

        String header = lines.getFirst();
        assertTrue(header.contains("scenario"), "Header should contain 'scenario'");
        assertTrue(header.contains("policy"), "Header should contain 'policy'");
        assertTrue(header.contains("responseTimeSec"), "Header should contain 'responseTimeSec'");

        // Data rows should reference the scenario and policy
        String firstRow = lines.get(1);
        assertTrue(firstRow.startsWith("test,B1,42"), "Data row should start with scenario,policy,seed");
    }

    @Test
    void runMultiplePoliciesAndSeeds(@TempDir Path tempDir) throws IOException {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("multi");
        config.setDurationHours(4);
        config.setBaseDemandPerHour(3.0);

        ExperimentRunner runner = new ExperimentRunner(
                List.of(config),
                List.of("B1", "B2"),
                List.of(42L, 43L),
                tempDir
        );
        runner.run();

        Path csvFile = tempDir.resolve("multi_results.csv");
        assertTrue(Files.exists(csvFile));

        List<String> lines = Files.readAllLines(csvFile);
        // Header + data for 2 policies × 2 seeds = at least 5 lines (header + some incidents each)
        assertTrue(lines.size() > 1, "Should have header + data rows");
    }

    @Test
    void runAllPoliciesIncludingP3AndAblations(@TempDir Path tempDir) throws IOException {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("all_policies");
        config.setDurationHours(4);
        config.setBaseDemandPerHour(3.0);

        ExperimentRunner runner = new ExperimentRunner(
                List.of(config),
                List.of("B1", "B2", "P1", "P2", "P3", "P3-no-redeploy", "P3-no-hosp", "P3-no-coverage", "P3-no-fatigue"),
                List.of(43L),
                tempDir
        );
        runner.run();

        Path csvFile = tempDir.resolve("all_policies_results.csv");
        assertTrue(Files.exists(csvFile));

        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.size() > 1, "Should have results for all policies");
    }
}
