package com.academic.annotation.service;

import com.academic.annotation.model.TrainingRun;
import com.academic.annotation.repository.TrainingRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingService {

    private final TrainingRunRepository trainingRunRepository;
    private final ObjectMapper objectMapper;
    private final String pythonExecutable;

    public TrainingService(TrainingRunRepository trainingRunRepository,
                           ObjectMapper objectMapper,
                           @Value("${app.python.executable:python}") String pythonExecutable) {
        this.trainingRunRepository = trainingRunRepository;
        this.objectMapper = objectMapper;
        this.pythonExecutable = pythonExecutable;
    }

    public List<TrainingRun> history() {
        return trainingRunRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional
    public TrainingRun launchTraining() {
        TrainingRun run = new TrainingRun();
        run.setStartedAt(LocalDateTime.now());
        run.setStatus("RUNNING");
        run = trainingRunRepository.save(run);

        StringBuilder logs = new StringBuilder();
        try {
            File workingDirectory = new File(".").getCanonicalFile();
            ensurePythonScripts(workingDirectory.toPath());
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, "python/train.py");
            processBuilder.directory(workingDirectory);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            logs.append(output).append(System.lineSeparator()).append("Exit code: ").append(exitCode);
            if (exitCode == 0) {
                Path metricsPath = workingDirectory.toPath().resolve("python").resolve("metrics.json");
                JsonNode metrics = objectMapper.readTree(Files.readString(metricsPath));
                run.setAccuracy(metrics.path("accuracy").asDouble());
                run.setF1Score(metrics.path("f1Score").asDouble());
                run.setStatus("SUCCESS");
            } else {
                run.setStatus("FAILED");
            }
        } catch (Exception e) {
            logs.append("Training failed: ").append(e.getMessage());
            run.setStatus("FAILED");
        }
        run.setEndedAt(LocalDateTime.now());
        run.setLogs(logs.toString());
        return trainingRunRepository.save(run);
    }

    private void ensurePythonScripts(Path workingDirectory) throws Exception {
        Path pythonDirectory = workingDirectory.resolve("python");
        Files.createDirectories(pythonDirectory);
        copyResourceIfMissing("python/train.py", pythonDirectory.resolve("train.py"));
        copyResourceIfMissing("python/test.py", pythonDirectory.resolve("test.py"));
        copyResourceIfMissing("python/metrics.json", pythonDirectory.resolve("metrics.json"));
    }

    private void copyResourceIfMissing(String resourcePath, Path destination) throws Exception {
        if (Files.exists(destination)) {
            return;
        }
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
