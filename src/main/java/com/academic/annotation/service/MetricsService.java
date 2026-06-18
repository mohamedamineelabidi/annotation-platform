package com.academic.annotation.service;

import com.academic.annotation.dto.MetricResult;
import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inter-annotator agreement metrics (specification: UC7 - advanced agreement).
 * Operates on the existing Annotation table, grouped by dataset couple.
 */
@Service
public class MetricsService {

    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;

    public MetricsService(DatasetItemRepository datasetItemRepository,
                          AnnotationRepository annotationRepository) {
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
    }

    /**
     * Builds the per-couple rating maps (annotator -> label) for a dataset,
     * keeping only couples annotated by at least two annotators.
     */
    private List<Map<Long, String>> buildUnits(Dataset dataset) {
        List<Map<Long, String>> units = new ArrayList<>();
        for (DatasetItem item : datasetItemRepository.findByDatasetOrderById(dataset)) {
            Map<Long, String> ratings = new LinkedHashMap<>();
            for (Annotation annotation : annotationRepository.findByDatasetItem(item)) {
                User annotator = annotation.getAnnotator();
                ratings.put(annotator.getId(), annotation.getLabel().getName());
            }
            if (ratings.size() >= 2) {
                units.add(ratings);
            }
        }
        return units;
    }

    public List<MetricResult> metricsFor(Dataset dataset) {
        List<Map<Long, String>> units = buildUnits(dataset);
        List<MetricResult> results = new ArrayList<>();

        if (units.isEmpty()) {
            results.add(new MetricResult("Couples comparables", "0",
                    "Aucun couple annoté par au moins deux annotateurs."));
            return results;
        }

        results.add(new MetricResult("Couples comparables", String.valueOf(units.size()),
                "Couples annotés par au moins deux annotateurs."));
        results.add(new MetricResult("Accord en pourcentage", format(percentAgreement(units)),
                "Proportion moyenne de paires d'annotateurs en accord par couple."));
        results.add(new MetricResult("Kappa de Cohen", cohenKappa(units),
                "Accord corrigé du hasard (exactement deux annotateurs)."));
        results.add(new MetricResult("Kappa de Fleiss", fleissKappa(units),
                "Accord corrigé du hasard (plusieurs annotateurs, n constant)."));
        results.add(new MetricResult("Alpha de Krippendorff", krippendorffAlpha(units),
                "Accord corrigé du hasard (nominal, supporte un nombre variable d'annotateurs)."));
        return results;
    }

    /* ---------------- Percent agreement (pairwise, averaged per unit) -------- */

    private double percentAgreement(List<Map<Long, String>> units) {
        double sum = 0.0;
        int counted = 0;
        for (Map<Long, String> unit : units) {
            List<String> values = new ArrayList<>(unit.values());
            int m = values.size();
            int pairs = 0;
            int agree = 0;
            for (int i = 0; i < m; i++) {
                for (int j = i + 1; j < m; j++) {
                    pairs++;
                    if (values.get(i).equals(values.get(j))) {
                        agree++;
                    }
                }
            }
            if (pairs > 0) {
                sum += (double) agree / pairs;
                counted++;
            }
        }
        return counted == 0 ? 0.0 : sum / counted;
    }

    /* ---------------- Cohen's Kappa (exactly two annotators) ----------------- */

    private String cohenKappa(List<Map<Long, String>> units) {
        // Collect the distinct annotators appearing across the dataset.
        Map<Long, Integer> annotators = new LinkedHashMap<>();
        for (Map<Long, String> unit : units) {
            for (Long id : unit.keySet()) {
                annotators.put(id, annotators.getOrDefault(id, 0) + 1);
            }
        }
        if (annotators.size() != 2) {
            return "N/A";
        }
        List<Long> ids = new ArrayList<>(annotators.keySet());
        Long a = ids.get(0);
        Long b = ids.get(1);

        int total = 0;
        int agree = 0;
        Map<String, Integer> countA = new HashMap<>();
        Map<String, Integer> countB = new HashMap<>();
        for (Map<Long, String> unit : units) {
            String va = unit.get(a);
            String vb = unit.get(b);
            if (va == null || vb == null) {
                continue;
            }
            total++;
            if (va.equals(vb)) {
                agree++;
            }
            countA.merge(va, 1, Integer::sum);
            countB.merge(vb, 1, Integer::sum);
        }
        if (total == 0) {
            return "N/A";
        }
        double po = (double) agree / total;
        double pe = 0.0;
        for (Map.Entry<String, Integer> entry : countA.entrySet()) {
            double pa = (double) entry.getValue() / total;
            double pb = (double) countB.getOrDefault(entry.getKey(), 0) / total;
            pe += pa * pb;
        }
        if (1.0 - pe == 0.0) {
            return format(po == 1.0 ? 1.0 : 0.0);
        }
        return format((po - pe) / (1.0 - pe));
    }

    /* ---------------- Fleiss' Kappa (constant number of raters) -------------- */

    private String fleissKappa(List<Map<Long, String>> units) {
        // Fleiss requires a fixed number of ratings per unit: keep the modal count.
        Map<Integer, Integer> countBySize = new HashMap<>();
        for (Map<Long, String> unit : units) {
            countBySize.merge(unit.size(), 1, Integer::sum);
        }
        int n = 0;
        int best = -1;
        for (Map.Entry<Integer, Integer> entry : countBySize.entrySet()) {
            if (entry.getKey() >= 2 && entry.getValue() > best) {
                best = entry.getValue();
                n = entry.getKey();
            }
        }
        if (n < 2) {
            return "N/A";
        }
        List<Map<Long, String>> selected = new ArrayList<>();
        for (Map<Long, String> unit : units) {
            if (unit.size() == n) {
                selected.add(unit);
            }
        }
        int nUnits = selected.size();
        if (nUnits == 0) {
            return "N/A";
        }

        Map<String, Integer> categoryTotals = new LinkedHashMap<>();
        double sumPi = 0.0;
        for (Map<Long, String> unit : selected) {
            Map<String, Integer> counts = new HashMap<>();
            for (String value : unit.values()) {
                counts.merge(value, 1, Integer::sum);
                categoryTotals.merge(value, 1, Integer::sum);
            }
            double sumSquares = 0.0;
            for (int nij : counts.values()) {
                sumSquares += (double) nij * nij;
            }
            sumPi += (sumSquares - n) / ((double) n * (n - 1));
        }
        double pBar = sumPi / nUnits;

        double pe = 0.0;
        double denom = (double) nUnits * n;
        for (int total : categoryTotals.values()) {
            double pj = total / denom;
            pe += pj * pj;
        }
        if (1.0 - pe == 0.0) {
            return format(pBar == 1.0 ? 1.0 : 0.0);
        }
        return format((pBar - pe) / (1.0 - pe));
    }

    /* ---------------- Krippendorff's Alpha (nominal) ------------------------- */

    private String krippendorffAlpha(List<Map<Long, String>> units) {
        // Coincidence matrix built from pairable values within each unit.
        Map<String, Double> nc = new LinkedHashMap<>();
        double observedDisagreement = 0.0; // sum over c != k of o_ck
        double nTotal = 0.0;

        for (Map<Long, String> unit : units) {
            List<String> values = new ArrayList<>(unit.values());
            int m = values.size();
            if (m < 2) {
                continue;
            }
            double weight = 1.0 / (m - 1);
            for (int i = 0; i < m; i++) {
                nc.merge(values.get(i), weight * (m - 1), Double::sum); // each value pairs with (m-1) others
                nTotal += weight * (m - 1);
                for (int j = 0; j < m; j++) {
                    if (i != j && !values.get(i).equals(values.get(j))) {
                        observedDisagreement += weight;
                    }
                }
            }
        }
        if (nTotal < 2) {
            return "N/A";
        }
        double sumSquares = 0.0;
        for (double value : nc.values()) {
            sumSquares += value * value;
        }
        double expectedDisagreement = nTotal * nTotal - sumSquares; // sum over c != k of nc * nk
        if (expectedDisagreement == 0.0) {
            return format(observedDisagreement == 0.0 ? 1.0 : 0.0);
        }
        double alpha = 1.0 - (nTotal - 1.0) * (observedDisagreement / expectedDisagreement);
        return format(alpha);
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
