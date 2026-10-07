package com.h8.ems.simulator;

import com.h8.ems.simulator.config.ScenarioConfig;
import com.h8.ems.simulator.runner.ExperimentRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MANDATORY per rule #7: Simulator runs are deterministic for a given seed.
 * Runs the full pipeline twice with the same seed and verifies CSV byte-identical output.
 */
class DeterminismTest {

    @Test
    void sameSeedProducesIdenticalCsv(@TempDir Path tempDir) throws IOException {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("determinism");
        config.setDurationHours(4);
        config.setBaseDemandPerHour(3.0);

        Path outDir1 = tempDir.resolve("run1");
        Path outDir2 = tempDir.resolve("run2");

        // Run 1
        new ExperimentRunner(
                List.of(config),
                List.of("B1", "B2", "P1"),
                List.of(42L, 43L),
                outDir1
        ).run();

        // Run 2 — identical config and seeds
        new ExperimentRunner(
                List.of(config),
                List.of("B1", "B2", "P1"),
                List.of(42L, 43L),
                outDir2
        ).run();

        Path csv1 = outDir1.resolve("determinism_results.csv");
        Path csv2 = outDir2.resolve("determinism_results.csv");

        assertTrue(Files.exists(csv1), "Run 1 CSV should exist");
        assertTrue(Files.exists(csv2), "Run 2 CSV should exist");

        byte[] bytes1 = Files.readAllBytes(csv1);
        byte[] bytes2 = Files.readAllBytes(csv2);

        assertArrayEquals(bytes1, bytes2,
                "Two runs with the same seed must produce byte-identical CSV output");
    }

    @Test
    void differentSeedsProduceDifferentCsv(@TempDir Path tempDir) throws IOException {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("diffseed");
        config.setDurationHours(4);
        config.setBaseDemandPerHour(3.0);

        Path outDir1 = tempDir.resolve("run1");
        Path outDir2 = tempDir.resolve("run2");

        new ExperimentRunner(
                List.of(config),
                List.of("B1"),
                List.of(42L),
                outDir1
        ).run();

        new ExperimentRunner(
                List.of(config),
                List.of("B1"),
                List.of(99L),
                outDir2
        ).run();

        byte[] bytes1 = Files.readAllBytes(outDir1.resolve("diffseed_results.csv"));
        byte[] bytes2 = Files.readAllBytes(outDir2.resolve("diffseed_results.csv"));

        assertFalse(java.util.Arrays.equals(bytes1, bytes2),
                "Different seeds should produce different CSV output");
    }
}
