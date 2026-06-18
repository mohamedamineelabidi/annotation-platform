package com.academic.annotation.service;

import com.academic.annotation.model.Annotation;
import com.academic.annotation.repository.AnnotationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

@Service
public class ExportService {

    private final AnnotationRepository annotationRepository;

    public ExportService(AnnotationRepository annotationRepository) {
        this.annotationRepository = annotationRepository;
    }

    public String exportAnnotationsCsv() {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("id", "text", "classe", "annotateur", "date_annotation")
                .get())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Annotation annotation : annotationRepository.findAll()) {
                printer.printRecord(
                        annotation.getDatasetItem().getExternalId(),
                        annotation.getDatasetItem().displayText(),
                        annotation.getLabel().getName(),
                        annotation.getAnnotator().getUsername(),
                        annotation.getAnnotationDate() == null ? "" : annotation.getAnnotationDate().format(formatter)
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate CSV", e);
        }
        return writer.toString();
    }
}
