package com.h8.ems.simulator.runner;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.config.ScenarioConfig;
import com.h8.ems.simulator.engine.*;
import com.h8.ems.simulator.metrics.MetricsAggregator;
import com.h8.ems.simulator.policy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs experiments: scenarios × policies × seeds.
 * Writes detailed incident CSVs, consolidated summary CSVs, aggregated stats with 95% CIs,
 * paired statistical hypothesis tests, and markdown reports.
 */
public final class ExperimentRunner {

    private static final Logger log = LoggerFactory.getLogger(ExperimentRunner.class);

    private final List<ScenarioConfig> scenarios;
    private final List<String> policyNames;
    private final List<Long> seeds;
    private final Path outputDir;

    public ExperimentRunner(List<ScenarioConfig> scenarios, List<String> policyNames,
                             List<Long> seeds, Path outputDir) {
        this.scenarios = scenarios;
        this.policyNames = policyNames;
        this.seeds = seeds;
        this.outputDir = outputDir;
    }

    public void run() throws IOException {
        Files.createDirectories(outputDir);
        List<MetricsAggregator.Summary> allSummaries = new ArrayList<>();

        for (ScenarioConfig scenario : scenarios) {
            Path csvFile = outputDir.resolve(scenario.getName() + "_results.csv");
            log.info("Running scenario: {} → {}", scenario.getName(), csvFile);

            try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
                writer.write(SimEngine.IncidentResult.csvHeader());
                writer.newLine();

                for (long seed : seeds) {
                    // Generate ONE incident stream per (scenario, seed) — CRN rule
                    DemandGenerator gen = new DemandGenerator(scenario, seed);
                    Instant simStart = Instant.parse("2025-01-01T08:00:00Z");
                    List<IncidentDraw> draws = gen.generate(simStart);
                    log.info("  Seed {} → {} incidents generated", seed, draws.size());

                    for (String policyName : policyNames) {
                        Policy policy = createPolicy(policyName, scenario);
                        SimState state = initState(scenario, simStart);
                        TravelModel travelModel = new TravelModel(new HaversineEta());
                        HospitalModel hospitalModel = initHospitals(scenario);

                        List<GeoPoint> zoneCentroids = scenario.getZones().stream()
                                .map(ScenarioConfig.ZoneConfig::toGeoPoint).toList();
                        CoverageModel coverageModel = new CoverageModel(
                                scenario.getCoverageRadiusKm(), zoneCentroids);
                        RedeploymentPlanner redeployPlanner = new RedeploymentPlanner(coverageModel);

                        SimEngine engine = new SimEngine(state, travelModel, hospitalModel,
                                policy, scenario, coverageModel, redeployPlanner);
                        engine.scheduleIncidents(draws);

                        List<SimEngine.IncidentResult> results = engine.run();

                        for (var r : results) {
                            writer.write(r.toCsvRow(scenario.getName(), seed));
                            writer.newLine();
                        }

                        var summary = MetricsAggregator.summarize(
                                scenario.getName(), policyName, seed, results,
                                scenario.getResponseTargetSeconds());
                        allSummaries.add(summary);
                        log.info("    {} | {}", policyName, summary);
                    }
                }
            }
            log.info("Scenario {} complete → {}", scenario.getName(), csvFile);
        }

        // Export consolidated summaries across all runs
        writeSummariesCsv(allSummaries);

        // Export aggregated metrics with 95% Confidence Intervals
        List<MetricsAggregator.AggregatedStats> aggList = writeAggregatedMetricsCsv(allSummaries);

        // Export paired hypothesis test results (CRN paired comparisons)
        List<MetricsAggregator.PairedComparison> pairedList = writePairedComparisonsCsv(allSummaries);

        // Generate complete markdown report
        writeMarkdownReport(aggList, pairedList);
    }

    private void writeSummariesCsv(List<MetricsAggregator.Summary> summaries) throws IOException {
        Path summaryFile = outputDir.resolve("experiment_summaries.csv");
        try (BufferedWriter w = Files.newBufferedWriter(summaryFile)) {
            w.write(MetricsAggregator.Summary.csvHeader());
            w.newLine();
            for (var s : summaries) {
                w.write(s.toCsvRow());
                w.newLine();
            }
        }
        log.info("Wrote consolidated run summaries to {}", summaryFile);
    }

    private List<MetricsAggregator.AggregatedStats> writeAggregatedMetricsCsv(List<MetricsAggregator.Summary> summaries) throws IOException {
        Path aggFile = outputDir.resolve("aggregated_metrics.csv");
        List<MetricsAggregator.AggregatedStats> results = new ArrayList<>();

        Map<String, Map<String, List<MetricsAggregator.Summary>>> grouped = summaries.stream()
                .collect(Collectors.groupingBy(MetricsAggregator.Summary::scenario,
                        Collectors.groupingBy(MetricsAggregator.Summary::policy)));

        try (BufferedWriter w = Files.newBufferedWriter(aggFile)) {
            w.write(MetricsAggregator.AggregatedStats.csvHeader());
            w.newLine();

            for (ScenarioConfig sc : scenarios) {
                String scName = sc.getName();
                if (!grouped.containsKey(scName)) continue;
                for (String pol : policyNames) {
                    List<MetricsAggregator.Summary> list = grouped.get(scName).get(pol);
                    if (list != null && !list.isEmpty()) {
                        MetricsAggregator.AggregatedStats stats = MetricsAggregator.aggregate(scName, pol, list);
                        results.add(stats);
                        w.write(stats.toCsvRow());
                        w.newLine();
                    }
                }
            }
        }
        log.info("Wrote aggregated metrics with 95% CI to {}", aggFile);
        return results;
    }

    private List<MetricsAggregator.PairedComparison> writePairedComparisonsCsv(List<MetricsAggregator.Summary> summaries) throws IOException {
        Path pairedFile = outputDir.resolve("paired_significance_tests.csv");
        List<MetricsAggregator.PairedComparison> results = new ArrayList<>();

        Map<String, Map<String, List<MetricsAggregator.Summary>>> grouped = summaries.stream()
                .collect(Collectors.groupingBy(MetricsAggregator.Summary::scenario,
                        Collectors.groupingBy(MetricsAggregator.Summary::policy)));

        try (BufferedWriter w = Files.newBufferedWriter(pairedFile)) {
            w.write(MetricsAggregator.PairedComparison.csvHeader());
            w.newLine();

            for (ScenarioConfig sc : scenarios) {
                String scName = sc.getName();
                if (!grouped.containsKey(scName)) continue;
                Map<String, List<MetricsAggregator.Summary>> pMap = grouped.get(scName);

                List<MetricsAggregator.Summary> b1 = pMap.get("B1");
                List<MetricsAggregator.Summary> b2 = pMap.get("B2");
                List<MetricsAggregator.Summary> p1 = pMap.get("P1");
                List<MetricsAggregator.Summary> p2 = pMap.get("P2");
                List<MetricsAggregator.Summary> p3 = pMap.get("P3");

                if (b1 != null && p3 != null) {
                    var cmp = MetricsAggregator.comparePaired(scName, "B1", "P3", b1, p3);
                    results.add(cmp);
                    w.write(cmp.toCsvRow());
                    w.newLine();
                }
                if (b2 != null && p3 != null) {
                    var cmp = MetricsAggregator.comparePaired(scName, "B2", "P3", b2, p3);
                    results.add(cmp);
                    w.write(cmp.toCsvRow());
                    w.newLine();
                }
                if (p1 != null && p3 != null) {
                    var cmp = MetricsAggregator.comparePaired(scName, "P1", "P3", p1, p3);
                    results.add(cmp);
                    w.write(cmp.toCsvRow());
                    w.newLine();
                }
                if (p2 != null && p3 != null) {
                    var cmp = MetricsAggregator.comparePaired(scName, "P2", "P3", p2, p3);
                    results.add(cmp);
                    w.write(cmp.toCsvRow());
                    w.newLine();
                }
            }
        }
        log.info("Wrote paired hypothesis test comparisons to {}", pairedFile);
        return results;
    }

    private void writeMarkdownReport(List<MetricsAggregator.AggregatedStats> statsList,
                                     List<MetricsAggregator.PairedComparison> pairedList) throws IOException {
        Path reportFile = outputDir.resolve("experiments_report.md");
        try (BufferedWriter w = Files.newBufferedWriter(reportFile)) {
            w.write("# H8 EMS Platform — Scientific Experiment Results (Phase 8)\n\n");
            w.write("## 1. Overview\n");
            w.write("This report presents discrete-event simulation results across experimental scenarios S1–S5 ");
            w.write("evaluating baseline policies (B1, B2) against proposed capability-aware and redeployment policies (P1, P2, P3).\n");
            w.write("All runs adhere strictly to the **Common Random Numbers (CRN)** protocol for variance reduction.\n\n");

            w.write("## 2. Policy Performance Summary (Mean Response Time with 95% Confidence Intervals)\n\n");
            w.write("| Scenario | Policy | Seeds | Mean Response (s) | 95% CI | p90 (s) | Target (≤8m) % | Handover Delay (s) | ALS Match % | Total Mission (s) |\n");
            w.write("|----------|--------|-------|-------------------|--------|---------|----------------|--------------------|-------------|-------------------|\n");
            for (var s : statsList) {
                w.write("| %s | %s | %d | %.1f s | [%.1f, %.1f] | %.1f s | %.1f%% | %.1f s | %.1f%% | %.1f s |\n".formatted(
                        s.scenario(), s.policy(), s.sampleSize(),
                        s.meanResponseSec(), s.ci95Lower(), s.ci95Upper(),
                        s.p90ResponseSec(),
                        s.meanWithinTargetPct() * 100.0,
                        s.meanHandoverSec(),
                        s.meanAlsMatchPct() * 100.0,
                        s.meanTotalTimeSec()
                ));
            }
            w.write("\n");

            w.write("## 3. Paired Statistical Hypothesis Tests (CRN Paired Differences)\n\n");
            w.write("| Scenario | Baseline | Proposed | Paired Seeds | Mean Reduction | Relative Gain | t-Statistic | Significance |\n");
            w.write("|----------|----------|----------|--------------|----------------|---------------|-------------|--------------|\n");
            for (var p : pairedList) {
                w.write("| %s | %s | %s | %d | %.1f s | %s | %.3f | **%s** |\n".formatted(
                        p.scenario(), p.baselinePolicy(), p.proposedPolicy(), p.pairs(),
                        p.meanReductionSec(), p.percentImprovement(), p.tStatistic(), p.significance()
                ));
            }
            w.write("\n");
        }
        log.info("Wrote experiments markdown report to {}", reportFile);
    }

    private Policy createPolicy(String name, ScenarioConfig config) {
        List<GeoPoint> zoneCentroids = config.getZones().stream()
                .map(ScenarioConfig.ZoneConfig::toGeoPoint).toList();
        CoverageModel coverageModel = new CoverageModel(config.getCoverageRadiusKm(), zoneCentroids);
        DispatchScorer scorer = new DispatchScorer(coverageModel);
        DestinationRanker ranker = new DestinationRanker();

        return switch (name) {
            case "B1" -> new NearestPolicy();
            case "B2" -> new NearestAlsAwarePolicy();
            case "P1" -> new ScorerPolicy(scorer);
            case "P2" -> new ScorerWithHospitalPolicy(scorer, ranker, 15);
            case "P3" -> new FullPolicy(scorer, ranker, 15);
            case "P3-no-redeploy" -> new FullPolicy(scorer, ranker, 15,
                    new FullPolicy.AblationFlags(true, false, false, false));
            case "P3-no-hosp" -> new FullPolicy(scorer, ranker, 15,
                    new FullPolicy.AblationFlags(false, true, false, false));
            case "P3-no-coverage" -> {
                ScorerParams p = new ScorerParams(0.40, 0.25, 0.10, 0.0, 0.10, 10.0, 90.0, 8.0);
                yield new FullPolicy(new DispatchScorer(p, coverageModel), ranker, 15,
                        new FullPolicy.AblationFlags(false, false, true, false));
            }
            case "P3-no-fatigue" -> {
                ScorerParams p = new ScorerParams(0.40, 0.25, 0.0, 0.15, 0.10, 10.0, 90.0, 8.0);
                yield new FullPolicy(new DispatchScorer(p, coverageModel), ranker, 15,
                        new FullPolicy.AblationFlags(false, false, false, true));
            }
            default -> throw new IllegalArgumentException("Unknown policy: " + name);
        };
    }

    private SimState initState(ScenarioConfig config, Instant simStart) {
        SimState state = new SimState(simStart);
        int unitCounter = 0;

        if (!config.getStations().isEmpty()) {
            for (var station : config.getStations()) {
                UUID stationId = UUID.nameUUIDFromBytes(station.getName().getBytes());
                for (int i = 0; i < station.getUnits(); i++) {
                    UUID unitId = UUID.nameUUIDFromBytes(
                            (station.getName() + "-" + i).getBytes());
                    UnitType type = (unitCounter < config.getFleetSize() * config.getAlsFraction())
                            ? UnitType.ALS : UnitType.BLS;
                    state.addUnit(unitId, station.getName().substring(0, Math.min(3, station.getName().length()))
                                    + (unitCounter + 1),
                            type, station.toGeoPoint(), stationId);
                    unitCounter++;
                }
            }
        } else {
            // Auto-generate units spread across the bounding box
            Random rng = new Random(42);
            for (int i = 0; i < config.getFleetSize(); i++) {
                UUID unitId = UUID.nameUUIDFromBytes(("unit-" + i).getBytes());
                UUID stationId = UUID.nameUUIDFromBytes(("station-" + (i % 5)).getBytes());
                UnitType type = (i < config.getFleetSize() * config.getAlsFraction())
                        ? UnitType.ALS : UnitType.BLS;
                double lat = config.getMinLat() + rng.nextDouble() * (config.getMaxLat() - config.getMinLat());
                double lon = config.getMinLon() + rng.nextDouble() * (config.getMaxLon() - config.getMinLon());
                GeoPoint pos = new GeoPoint(lat, lon);
                state.addUnit(unitId, "U" + (i + 1), type, pos, stationId);
            }
        }
        return state;
    }

    private HospitalModel initHospitals(ScenarioConfig config) {
        HospitalModel model = new HospitalModel();
        if (!config.getHospitals().isEmpty()) {
            for (var h : config.getHospitals()) {
                UUID id = UUID.nameUUIDFromBytes(h.getName().getBytes());
                model.initHospital(id, h.getName(), h.toGeoPoint(),
                        h.toClinicalNeeds(), h.getEdBeds(), h.getIcuBeds(), h.getVentilators());
            }
        } else {
            // Default: 5 hospitals spread across the area
            for (int i = 0; i < 5; i++) {
                UUID id = UUID.nameUUIDFromBytes(("hospital-" + i).getBytes());
                double lat = config.getMinLat() + (i + 0.5) / 5.0 * (config.getMaxLat() - config.getMinLat());
                double lon = config.getMinLon() + 0.5 * (config.getMaxLon() - config.getMinLon());
                model.initHospital(id, "Hospital-" + (i + 1), new GeoPoint(lat, lon),
                        Set.of(ClinicalNeed.GENERAL, ClinicalNeed.TRAUMA, ClinicalNeed.CARDIAC),
                        20, 5, 3);
            }
        }
        return model;
    }
}
