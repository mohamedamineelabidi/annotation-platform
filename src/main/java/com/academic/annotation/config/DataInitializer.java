package com.academic.annotation.config;

import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.Role;
import com.academic.annotation.model.TaskType;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.DatasetRepository;
import com.academic.annotation.service.AssignmentService;
import com.academic.annotation.service.DatasetService;
import com.academic.annotation.service.TaskService;
import com.academic.annotation.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final DatasetService datasetService;
    private final AssignmentService assignmentService;
    private final DatasetItemRepository datasetItemRepository;
    private final DatasetRepository datasetRepository;
    private final TaskService taskService;

    public DataInitializer(UserService userService,
                           DatasetService datasetService,
                           AssignmentService assignmentService,
                           DatasetItemRepository datasetItemRepository,
                           DatasetRepository datasetRepository,
                           TaskService taskService) {
        this.userService = userService;
        this.datasetService = datasetService;
        this.assignmentService = assignmentService;
        this.datasetItemRepository = datasetItemRepository;
        this.datasetRepository = datasetRepository;
        this.taskService = taskService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userService.ensureUser("admin", "admin", Role.ADMIN);
        userService.ensureUser("user1", "user1", Role.ANNOTATOR);
        userService.ensureUser("user2", "user2", Role.ANNOTATOR);
        userService.ensureUser("user3", "user3", Role.ANNOTATOR);

        datasetService.createLabel("positive", TaskType.TEXT_CLASSIFICATION);
        datasetService.createLabel("negative", TaskType.TEXT_CLASSIFICATION);
        datasetService.createLabel("similar", TaskType.TEXT_PAIR);
        datasetService.createLabel("not_similar", TaskType.TEXT_PAIR);
        datasetService.createLabel("entails", TaskType.NLI);
        datasetService.createLabel("contradiction", TaskType.NLI);
        datasetService.createLabel("neutral", TaskType.NLI);

        if (datasetItemRepository.count() == 0) {
            datasetService.createItem("s1", "The product is excellent and easy to use.", null, TaskType.TEXT_CLASSIFICATION);
            datasetService.createItem("s2", "The service was slow and disappointing.", null, TaskType.TEXT_CLASSIFICATION);
            datasetService.createItem("s3", "This course helped me understand NLP basics.", null, TaskType.TEXT_CLASSIFICATION);
            datasetService.createItem("p1", "The sky is blue today.", "The weather is clear.", TaskType.TEXT_PAIR);
            datasetService.createItem("p2", "A cat is sleeping on the sofa.", "A vehicle is parked outside.", TaskType.TEXT_PAIR);
            datasetService.createItem("n1", "A student is reading a book.", "Someone is studying.", TaskType.NLI);
        }

        assignmentService.assignAllItemsToAnnotators(3);

        // Sample dataset (specification model) assigned to two annotators so that
        // dataset details, tasks and agreement metrics have data to display.
        if (datasetRepository.count() == 0) {
            Dataset sample = datasetService.createDatasetProgrammatic(
                    "Avis produits (démo)",
                    List.of("positif", "négatif", "neutre"),
                    "Jeu de données de démonstration pour la classification d'avis.",
                    TaskType.TEXT_CLASSIFICATION,
                    "admin",
                    List.of(
                            new String[]{"1", "Ce produit est excellent, je recommande vivement."},
                            new String[]{"2", "Livraison en retard et service décevant."},
                            new String[]{"3", "Le produit fonctionne comme décrit."},
                            new String[]{"4", "Qualité moyenne pour le prix."},
                            new String[]{"5", "Très satisfait de mon achat, rien à redire."},
                            new String[]{"6", "Je ne sais pas trop quoi en penser."}
                    ));
            User user1 = userService.findByUsername("user1");
            User user2 = userService.findByUsername("user2");
            taskService.assignAnnotators(sample, List.of(user1, user2), LocalDate.now().plusDays(14));
        }
    }
}
