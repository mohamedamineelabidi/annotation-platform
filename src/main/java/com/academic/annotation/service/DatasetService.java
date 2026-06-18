package com.academic.annotation.service;

import com.academic.annotation.dto.DatasetSummary;
import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetClass;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.Task;
import com.academic.annotation.model.TaskType;
import com.academic.annotation.repository.DatasetClassRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.DatasetRepository;
import com.academic.annotation.repository.LabelRepository;
import com.academic.annotation.repository.TaskItemRepository;
import com.academic.annotation.repository.TaskRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DatasetService {

    private final DatasetItemRepository datasetItemRepository;
    private final LabelRepository labelRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetClassRepository datasetClassRepository;
    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;

    public DatasetService(DatasetItemRepository datasetItemRepository,
                          LabelRepository labelRepository,
                          DatasetRepository datasetRepository,
                          DatasetClassRepository datasetClassRepository,
                          TaskRepository taskRepository,
                          TaskItemRepository taskItemRepository) {
        this.datasetItemRepository = datasetItemRepository;
        this.labelRepository = labelRepository;
        this.datasetRepository = datasetRepository;
        this.datasetClassRepository = datasetClassRepository;
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
    }

    public List<DatasetItem> findAllItems() {
        return datasetItemRepository.findAll();
    }

    public List<Label> findAllLabels() {
        return labelRepository.findAllByOrderByTaskTypeAscNameAsc();
    }

    public List<Label> findLabelsFor(TaskType taskType) {
        return labelRepository.findByTaskTypeOrderByName(taskType);
    }

    @Transactional
    public int importCsv(MultipartFile file, TaskType taskType) throws IOException {
        int imported = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {
            validateHeaders(parser.getHeaderMap().keySet());
            for (CSVRecord record : parser) {
                Map<String, String> row = normalizeRow(record.toMap());
                String externalId = value(row, "id");
                String text1 = value(row, "text");
                if (text1.isBlank()) {
                    text1 = value(row, "text1");
                }
                String text2 = value(row, "text2");
                if (externalId.isBlank() || text1.isBlank() || datasetItemRepository.findByExternalId(externalId).isPresent()) {
                    continue;
                }
                DatasetItem item = new DatasetItem();
                item.setExternalId(externalId);
                item.setText1(text1);
                item.setText2(text2.isBlank() ? null : text2);
                item.setTaskType(taskType);
                datasetItemRepository.save(item);
                imported++;
            }
        }
        return imported;
    }

    @Transactional
    public Label createLabel(String name, TaskType taskType) {
        return labelRepository.findByNameAndTaskType(name, taskType).orElseGet(() -> {
            Label label = new Label();
            label.setName(name);
            label.setTaskType(taskType);
            return labelRepository.save(label);
        });
    }

    @Transactional
    public void deleteLabel(Long id) {
        labelRepository.deleteById(id);
    }

    @Transactional
    public DatasetItem createItem(String externalId, String text1, String text2, TaskType taskType) {
        return datasetItemRepository.findByExternalId(externalId).orElseGet(() -> {
            DatasetItem item = new DatasetItem();
            item.setExternalId(externalId);
            item.setText1(text1);
            item.setText2(text2);
            item.setTaskType(taskType);
            return datasetItemRepository.save(item);
        });
    }

    /* ----------------------------------------------------------------------
     * Dataset management (specification: UC2 - create dataset with classes).
     * A Dataset groups its own couples and a dynamic list of classes. Classes
     * are mirrored into the existing Label table so that annotations, agreement
     * metrics and spam detection keep working unchanged.
     * ---------------------------------------------------------------------- */

    @Transactional
    public Dataset createDataset(MultipartFile file, String name, String classesRaw, String description,
                                 TaskType taskType, String createdBy) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom du dataset est obligatoire.");
        }
        List<String> classNames = parseClasses(classesRaw);
        if (classNames.size() < 2) {
            throw new IllegalArgumentException("Au moins deux classes sont requises (séparées par ;).");
        }

        Dataset dataset = new Dataset();
        dataset.setName(name.trim());
        dataset.setDescription(description == null || description.isBlank() ? null : description.trim());
        dataset.setTaskType(taskType);
        dataset.setCreatedBy(createdBy);
        for (String className : classNames) {
            dataset.addClass(new DatasetClass(className));
            createLabel(className, taskType);
        }
        datasetRepository.save(dataset);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {
            validateHeaders(parser.getHeaderMap().keySet());
            for (CSVRecord record : parser) {
                Map<String, String> row = normalizeRow(record.toMap());
                String csvId = value(row, "id");
                String text1 = value(row, "text");
                if (text1.isBlank()) {
                    text1 = value(row, "text1");
                }
                String text2 = value(row, "text2");
                if (csvId.isBlank() || text1.isBlank()) {
                    continue;
                }
                DatasetItem item = new DatasetItem();
                item.setExternalId("D" + dataset.getId() + "-" + csvId);
                item.setText1(text1);
                item.setText2(text2.isBlank() ? null : text2);
                item.setTaskType(taskType);
                item.setDataset(dataset);
                datasetItemRepository.save(item);
            }
        }
        return dataset;
    }

    private List<String> parseClasses(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(";"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Programmatic dataset creation used for seeding sample data. Each row is
     * {externalId, text1, text2 (optional)}.
     */
    @Transactional
    public Dataset createDatasetProgrammatic(String name, List<String> classNames, String description,
                                             TaskType taskType, String createdBy, List<String[]> rows) {
        Dataset dataset = new Dataset();
        dataset.setName(name);
        dataset.setDescription(description);
        dataset.setTaskType(taskType);
        dataset.setCreatedBy(createdBy);
        for (String className : classNames) {
            dataset.addClass(new DatasetClass(className));
            createLabel(className, taskType);
        }
        datasetRepository.save(dataset);
        for (String[] row : rows) {
            DatasetItem item = new DatasetItem();
            item.setExternalId("D" + dataset.getId() + "-" + row[0]);
            item.setText1(row[1]);
            item.setText2(row.length > 2 ? row[2] : null);
            item.setTaskType(taskType);
            item.setDataset(dataset);
            datasetItemRepository.save(item);
        }
        return dataset;
    }

    public List<Dataset> findAllDatasets() {
        return datasetRepository.findAllByOrderByCreatedAtDesc();
    }

    public Dataset getDataset(Long id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset introuvable"));
    }

    public List<DatasetItem> itemsOf(Dataset dataset) {
        return datasetItemRepository.findByDatasetOrderById(dataset);
    }

    public long sizeOf(Dataset dataset) {
        return datasetItemRepository.countByDataset(dataset);
    }

    public List<DatasetClass> classesOf(Dataset dataset) {
        return datasetClassRepository.findByDatasetOrderById(dataset);
    }

    public double progressOf(Dataset dataset) {
        List<Task> tasks = taskRepository.findByDatasetAndActiveTrue(dataset);
        long total = 0;
        long done = 0;
        for (Task task : tasks) {
            total += taskItemRepository.countByTask(task);
            done += taskItemRepository.countByTaskAndCompleted(task, true);
        }
        return total == 0 ? 0.0 : (done * 100.0) / total;
    }

    public List<DatasetSummary> listDatasetSummaries() {
        List<DatasetSummary> summaries = new ArrayList<>();
        for (Dataset dataset : findAllDatasets()) {
            long size = sizeOf(dataset);
            double progress = progressOf(dataset);
            int annotators = taskRepository.findByDatasetAndActiveTrue(dataset).size();
            summaries.add(new DatasetSummary(dataset.getId(), dataset.getName(),
                    dataset.getTaskType().getDisplayName(), size, progress, annotators));
        }
        return summaries;
    }

    private String value(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null ? "" : value.trim();
    }

    private Map<String, String> normalizeRow(Map<String, String> row) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            normalized.put(normalizeHeader(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private void validateHeaders(Set<String> headers) {
        Set<String> normalizedHeaders = headers.stream()
                .map(this::normalizeHeader)
                .collect(Collectors.toSet());

        boolean hasId = normalizedHeaders.contains("id");
        boolean hasText = normalizedHeaders.contains("text") || normalizedHeaders.contains("text1");
        if (!hasId || !hasText) {
            throw new IllegalArgumentException("CSV headers must include id and text (or text1). Optional: text2.");
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }

        String normalized = header.replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        return normalized.replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
