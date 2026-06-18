package com.academic.annotation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "annotation", uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_item_id", "annotator_id"}))
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_item_id")
    private DatasetItem datasetItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annotator_id")
    private User annotator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id")
    private Label label;

    private LocalDateTime annotationDate;

    private long timeSpentSeconds;

    @PrePersist
    void prePersist() {
        if (annotationDate == null) {
            annotationDate = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatasetItem getDatasetItem() {
        return datasetItem;
    }

    public void setDatasetItem(DatasetItem datasetItem) {
        this.datasetItem = datasetItem;
    }

    public User getAnnotator() {
        return annotator;
    }

    public void setAnnotator(User annotator) {
        this.annotator = annotator;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public LocalDateTime getAnnotationDate() {
        return annotationDate;
    }

    public void setAnnotationDate(LocalDateTime annotationDate) {
        this.annotationDate = annotationDate;
    }

    public long getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(long timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}
