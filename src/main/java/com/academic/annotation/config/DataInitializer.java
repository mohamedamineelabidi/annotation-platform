package com.academic.annotation.config;

import com.academic.annotation.model.Role;
import com.academic.annotation.model.TaskType;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.service.AssignmentService;
import com.academic.annotation.service.DatasetService;
import com.academic.annotation.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final DatasetService datasetService;
    private final AssignmentService assignmentService;
    private final DatasetItemRepository datasetItemRepository;

    public DataInitializer(UserService userService,
                           DatasetService datasetService,
                           AssignmentService assignmentService,
                           DatasetItemRepository datasetItemRepository) {
        this.userService = userService;
        this.datasetService = datasetService;
        this.assignmentService = assignmentService;
        this.datasetItemRepository = datasetItemRepository;
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
    }
}
