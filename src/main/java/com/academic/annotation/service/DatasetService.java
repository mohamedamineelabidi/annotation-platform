package com.academic.annotation.service;

import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.TaskType;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.LabelRepository;
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

    public DatasetService(DatasetItemRepository datasetItemRepository, LabelRepository labelRepository) {
        this.datasetItemRepository = datasetItemRepository;
        this.labelRepository = labelRepository;
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
