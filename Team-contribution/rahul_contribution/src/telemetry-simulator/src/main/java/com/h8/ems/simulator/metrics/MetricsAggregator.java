package com.h8.ems.simulator.metrics;

import com.h8.ems.simulator.engine.SimEngine.IncidentResult;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates IncidentResult data into summary statistics, clinical appropriateness,
 * handover delays, confidence intervals, and hypothesis tests.
 */
public final class MetricsAggregator {

    public record Summary(
            String scenario, String policy, long seed,
            int totalIncidents, int servedIncidents, int unservedIncidents,
            double meanResponseSec, double medianResponseSec,
            double p90ResponseSec, double p95ResponseSec,
            double withinTargetPct,
            double meanHandoverSec, double meanTotalTimeSec,
            double alsMatchPct
    ) {
        public static String csvHeader() {
            return "scenario,policy,seed,totalIncidents,servedIncidents,unservedIncidents,meanResponseSec,medianResponseSec,p90ResponseSec,p95ResponseSec,withinTargetPct,meanHandoverSec,meanTotalTimeSec,alsMatchPct";
        }

        public String toCsvRow() {
            return "%s,%s,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f,%.4f".formatted(
                    scenario, policy, seed, totalIncidents, servedIncidents, unservedIncidents,
                    meanResponseSec, medianResponseSec, p90ResponseSec, p95ResponseSec, withinTargetPct,
                    meanHandoverSec, meanTotalTimeSec, alsMatchPct
            );
        }

        @Override
        public String toString() {
            return "%s | %s | seed=%d | served=%d/%d | resp=%.0fs (med=%.0fs p90=%.0fs) | target=%.1f%% | handover=%.0fs | alsMatch=%.1f%%"
                    .formatted(scenario, policy, seed, servedIncidents, totalIncidents,
                            meanResponseSec, medianResponseSec, p90ResponseSec,
                            withinTargetPct * 100.0, meanHandoverSec, alsMatchPct * 100.0);
        }
    }

    public record AggregatedStats(
            String scenario, String policy, int sampleSize,
            double meanResponseSec, double ci95HalfWidth,
            double ci95Lower, double ci95Upper,
            double medianResponseSec,
            double p90ResponseSec, double p95ResponseSec,
            double meanWithinTargetPct,
            double meanHandoverSec, double meanTotalTimeSec,
            double meanAlsMatchPct
    ) {
        public static String csvHeader() {
            return "scenario,policy,seeds,meanResponseSec,ci95Lower,ci95Upper,medianResponseSec,p90ResponseSec,p95ResponseSec,withinTargetPct,meanHandoverSec,meanTotalTimeSec,alsMatchPct";
        }

        public String toCsvRow() {
            return "%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f,%.4f".formatted(
                    scenario, policy, sampleSize,
                    meanResponseSec, ci95Lower, ci95Upper,
                    medianResponseSec, p90ResponseSec, p95ResponseSec,
                    meanWithinTargetPct, meanHandoverSec, meanTotalTimeSec, meanAlsMatchPct
            );
        }
    }

    public record PairedComparison(
            String scenario, String baselinePolicy, String proposedPolicy, int pairs,
            double meanReductionSec, double percentImprovement,
            double tStatistic, String significance
    ) {
        public static String csvHeader() {
            return "scenario,baseline,proposed,pairs,meanReductionSec,percentImprovement,tStatistic,significance";
        }

        public String toCsvRow() {
            return "%s,%s,%s,%d,%.2f,%.2f%%,%.4f,%s".formatted(
                    scenario, baselinePolicy, proposedPolicy, pairs,
                    meanReductionSec, percentImprovement, tStatistic, significance
            );
        }
    }

    /**
     * Compute summary statistics for a set of results.
     */
    public static Summary summarize(String scenario, String policy, long seed,
                                     List<IncidentResult> results, double targetSeconds) {
        List<IncidentResult> served = results.stream()
                .filter(IncidentResult::served)
                .toList();

        List<Double> responseTimes = served.stream()
                .map(IncidentResult::responseTimeSec)
                .filter(r -> r >= 0)
                .sorted()
                .toList();

        int total = results.size();
        int servedCount = served.size();
        int unserved = total - servedCount;

        if (responseTimes.isEmpty()) {
            return new Summary(scenario, policy, seed, total, 0, unserved,
                    0, 0, 0, 0, 0, 0, 0, 0);
        }

        double mean = responseTimes.stream().mapToDouble(d -> d).average().orElse(0);
        double median = percentile(responseTimes, 0.5);
        double p90 = percentile(responseTimes, 0.9);
        double p95 = percentile(responseTimes, 0.95);
        long withinTarget = responseTimes.stream().filter(r -> r <= targetSeconds).count();
        double withinPct = (double) withinTarget / responseTimes.size();

        double totalHandoverSec = 0;
        int handoverCount = 0;
        double totalMissionSec = 0;
        int missionCount = 0;
        int criticalCount = 0;
        int criticalAlsCount = 0;

        for (var r : served) {
            if (r.arrivedHospitalAt() != null && r.handedOverAt() != null) {
                totalHandoverSec += Duration.between(r.arrivedHospitalAt(), r.handedOverAt()).toSeconds();
                handoverCount++;
            }
            if (r.receivedAt() != null && r.handedOverAt() != null) {
                totalMissionSec += Duration.between(r.receivedAt(), r.handedOverAt()).toSeconds();
                missionCount++;
            }
            if ("CRITICAL".equalsIgnoreCase(r.severity()) || "EMERGENCY".equalsIgnoreCase(r.severity())) {
                criticalCount++;
                if ("ALS".equalsIgnoreCase(r.unitType())) {
                    criticalAlsCount++;
                }
            }
        }

        double meanHandover = handoverCount > 0 ? totalHandoverSec / handoverCount : 0.0;
        double meanMission = missionCount > 0 ? totalMissionSec / missionCount : 0.0;
        double alsMatch = criticalCount > 0 ? (double) criticalAlsCount / criticalCount : 1.0;

        return new Summary(scenario, policy, seed, total, servedCount, unserved,
                mean, median, p90, p95, withinPct, meanHandover, meanMission, alsMatch);
    }

    /**
     * Aggregate summaries across seeds for a single scenario and policy.
     */
    public static AggregatedStats aggregate(String scenario, String policy, List<Summary> summaries) {
        if (summaries.isEmpty()) {
            return new AggregatedStats(scenario, policy, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int n = summaries.size();
        double mean = summaries.stream().mapToDouble(Summary::meanResponseSec).average().orElse(0);
        double median = summaries.stream().mapToDouble(Summary::medianResponseSec).average().orElse(0);
        double p90 = summaries.stream().mapToDouble(Summary::p90ResponseSec).average().orElse(0);
        double p95 = summaries.stream().mapToDouble(Summary::p95ResponseSec).average().orElse(0);
        double withinPct = summaries.stream().mapToDouble(Summary::withinTargetPct).average().orElse(0);

        double meanHandover = summaries.stream().mapToDouble(Summary::meanHandoverSec).average().orElse(0);
        double meanMission = summaries.stream().mapToDouble(Summary::meanTotalTimeSec).average().orElse(0);
        double meanAlsMatch = summaries.stream().mapToDouble(Summary::alsMatchPct).average().orElse(0);

        double variance = summaries.stream()
                .mapToDouble(s -> Math.pow(s.meanResponseSec() - mean, 2))
                .sum() / (n > 1 ? (n - 1) : 1);
        double stdDev = Math.sqrt(variance);
        double se = stdDev / Math.sqrt(n);
        double tCrit = n >= 30 ? 1.960 : 2.045;
        double halfWidth = tCrit * se;

        return new AggregatedStats(
                scenario, policy, n,
                mean, halfWidth,
                Math.max(0, mean - halfWidth), mean + halfWidth,
                median, p90, p95, withinPct,
                meanHandover, meanMission, meanAlsMatch
        );
    }

    /**
     * Perform paired statistical comparison across matched seeds (Common Random Numbers).
     */
    public static PairedComparison comparePaired(
            String scenario, String baselinePolicy, String proposedPolicy,
            List<Summary> baselineSummaries, List<Summary> proposedSummaries) {

        Map<Long, Double> baseMap = baselineSummaries.stream()
                .collect(Collectors.toMap(Summary::seed, Summary::meanResponseSec, (a, b) -> a));
        Map<Long, Double> propMap = proposedSummaries.stream()
                .collect(Collectors.toMap(Summary::seed, Summary::meanResponseSec, (a, b) -> a));

        List<Double> diffs = new ArrayList<>();
        double baseSum = 0;
        double propSum = 0;

        for (Map.Entry<Long, Double> entry : baseMap.entrySet()) {
            Long seed = entry.getKey();
            if (propMap.containsKey(seed)) {
                double bVal = entry.getValue();
                double pVal = propMap.get(seed);
                diffs.add(bVal - pVal); // positive means proposed is faster
                baseSum += bVal;
                propSum += pVal;
            }
        }

        int pairs = diffs.size();
        if (pairs == 0) {
            return new PairedComparison(scenario, baselinePolicy, proposedPolicy, 0, 0, 0, 0, "N/A");
        }

        double meanDiff = diffs.stream().mapToDouble(d -> d).average().orElse(0);
        double meanBase = baseSum / pairs;
        double pctImprovement = meanBase > 0 ? (meanDiff / meanBase) * 100.0 : 0.0;

        double variance = diffs.stream()
                .mapToDouble(d -> Math.pow(d - meanDiff, 2))
                .sum() / (pairs > 1 ? (pairs - 1) : 1);
        double seDiff = Math.sqrt(variance) / Math.sqrt(pairs);
        double tStat = seDiff > 0 ? meanDiff / seDiff : 0.0;

        String significance = (Math.abs(tStat) > 3.5) ? "p < 0.001" :
                (Math.abs(tStat) > 2.7) ? "p < 0.01" :
                        (Math.abs(tStat) > 2.0) ? "p < 0.05" : "n.s.";

        return new PairedComparison(scenario, baselinePolicy, proposedPolicy, pairs,
                meanDiff, pctImprovement, tStat, significance);
    }

    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
