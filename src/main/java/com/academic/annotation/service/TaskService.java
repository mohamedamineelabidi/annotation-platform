package com.academic.annotation.service;

import com.academic.annotation.dto.AssignedAnnotator;
import com.academic.annotation.dto.TaskSummary;
import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.Task;
import com.academic.annotation.model.TaskItem;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.LabelRepository;
import com.academic.annotation.repository.TaskItemRepository;
import com.academic.annotation.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final LabelRepository labelRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskItemRepository taskItemRepository,
                       DatasetItemRepository datasetItemRepository,
                       AnnotationRepository annotationRepository,
                       LabelRepository labelRepository) {
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.labelRepository = labelRepository;
    }

    /**
     * UC3 - assign one or more annotators to a dataset. Every selected annotator
     * receives the full set of couples (overlap is required for inter-annotator
     * agreement metrics). Already assigned couples are not duplicated.
     */
    @Transactional
    public void assignAnnotators(Dataset dataset, List<User> annotators, LocalDate deadline) {
        List<DatasetItem> items = datasetItemRepository.findByDatasetOrderById(dataset);
        for (User annotator : annotators) {
            Task task = taskRepository.findByDatasetAndAnnotatorAndActiveTrue(dataset, annotator)
                    .orElseGet(() -> {
                        Task created = new Task();
                        created.setDataset(dataset);
                        created.setAnnotator(annotator);
                        created.setActive(true);
                        return created;
                    });
            task.setDeadline(deadline);
            taskRepository.save(task);
            for (DatasetItem item : items) {
                if (!taskItemRepository.existsByTaskAndDatasetItem(task, item)) {
                    TaskItem taskItem = new TaskItem();
                    taskItem.setTask(task);
                    taskItem.setDatasetItem(item);
                    taskItem.setCompleted(false);
                    taskItemRepository.save(taskItem);
                }
            }
        }
    }

    /**
     * UC3 - logical de-assignment. The task is deactivated and its pending couples
     * are removed, but already completed couples and their annotations are kept.
     */
    @Transactional
    public void removeAnnotator(Dataset dataset, User annotator) {
        taskRepository.findByDatasetAndAnnotatorAndActiveTrue(dataset, annotator).ifPresent(task -> {
            taskItemRepository.deleteByTaskAndCompletedFalse(task);
            task.setActive(false);
            taskRepository.save(task);
        });
    }

    public List<AssignedAnnotator> assignedAnnotators(Dataset dataset) {
        List<AssignedAnnotator> result = new ArrayList<>();
        for (Task task : taskRepository.findByDatasetAndActiveTrue(dataset)) {
            User annotator = task.getAnnotator();
            long size = taskItemRepository.countByTask(task);
            long done = taskItemRepository.countByTaskAndCompleted(task, true);
            double progress = size == 0 ? 0.0 : (done * 100.0) / size;
            result.add(new AssignedAnnotator(task.getId(), annotator.getId(), annotator.getUsername(),
                    annotator.getFirstName(), annotator.getLastName(), size, progress));
        }
        return result;
    }

    public List<TaskSummary> tasksFor(User annotator) {
        List<TaskSummary> result = new ArrayList<>();
        for (Task task : taskRepository.findByAnnotatorAndActiveTrueOrderByIdDesc(annotator)) {
            long size = taskItemRepository.countByTask(task);
            long done = taskItemRepository.countByTaskAndCompleted(task, true);
            double progress = size == 0 ? 0.0 : (done * 100.0) / size;
            result.add(new TaskSummary(task.getId(), task.getDataset().getName(),
                    task.getDeadline(), progress, size, done));
        }
        return result;
    }

    public Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));
    }

    public List<TaskItem> itemsOf(Task task) {
        return taskItemRepository.findByTaskOrderById(task);
    }

    public long sizeOf(Task task) {
        return taskItemRepository.countByTask(task);
    }

    public long completedOf(Task task) {
        return taskItemRepository.countByTaskAndCompleted(task, true);
    }

    /**
     * UC4 - record the annotator's choice for a couple. The label is mirrored into
     * the existing Annotation table so that statistics and agreement metrics work.
     */
    @Transactional
    public void annotate(Task task, Long taskItemId, String labelName) {
        TaskItem taskItem = taskItemRepository.findById(taskItemId)
                .orElseThrow(() -> new IllegalArgumentException("Couple introuvable"));
        if (!taskItem.getTask().getId().equals(task.getId())) {
            throw new IllegalArgumentException("Couple non rattaché à cette tâche");
        }
        DatasetItem item = taskItem.getDatasetItem();
        Label label = labelRepository.findByNameAndTaskType(labelName, item.getTaskType())
                .orElseThrow(() -> new IllegalArgumentException("Classe inconnue: " + labelName));
        User annotator = task.getAnnotator();
        Annotation annotation = annotationRepository.findByDatasetItemAndAnnotator(item, annotator)
                .orElseGet(Annotation::new);
        annotation.setDatasetItem(item);
        annotation.setAnnotator(annotator);
        annotation.setLabel(label);
        annotationRepository.save(annotation);
        if (!taskItem.isCompleted()) {
            taskItem.setCompleted(true);
            taskItemRepository.save(taskItem);
        }
    }
}
