package com.dedalus.amphi_integration.controller;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisJobStatus;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisSummary;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayResult;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisArchiveService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisImportSessionService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisJobService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
@Tag(name = "Log Analyzer API", description = "Upload and analyze EVAM log files into replayable API sequences")
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;
    private final LogAnalysisArchiveService logAnalysisArchiveService;
    private final LogAnalysisImportSessionService logAnalysisImportSessionService;
    private final LogAnalysisJobService logAnalysisJobService;
    private final EvamScenarioReplayService evamScenarioReplayService;

    public LogAnalysisController(
            LogAnalysisService logAnalysisService,
            LogAnalysisArchiveService logAnalysisArchiveService,
            LogAnalysisImportSessionService logAnalysisImportSessionService,
            LogAnalysisJobService logAnalysisJobService,
            EvamScenarioReplayService evamScenarioReplayService) {
        this.logAnalysisService = logAnalysisService;
        this.logAnalysisArchiveService = logAnalysisArchiveService;
        this.logAnalysisImportSessionService = logAnalysisImportSessionService;
        this.logAnalysisJobService = logAnalysisJobService;
        this.evamScenarioReplayService = evamScenarioReplayService;
    }

    @PostMapping(value = "/log-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeLog(@RequestParam("file") List<MultipartFile> files) throws IOException {
        List<MultipartFile> nonEmptyFiles = files == null
                ? List.of()
                : files.stream().filter(file -> !file.isEmpty()).toList();
        if (nonEmptyFiles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ingen loggfil valdes."));
        }

        List<MultipartFile> sortedFiles = sortFiles(nonEmptyFiles);
        String sourceLog = sortedFiles.size() == 1
                ? sortedFiles.get(0).getOriginalFilename()
                : "Folder upload: " + inferFolderLabel(sortedFiles) + " (" + sortedFiles.size() + " filer)";

        LogAnalysisResult result = logAnalysisArchiveService.save(
                sortedFiles.size() == 1
                        ? logAnalysisService.analyze(sourceLog, sortedFiles.get(0).getBytes())
                        : logAnalysisService.analyzeMany(
                                sourceLog,
                                sortedFiles.stream()
                                        .map(file -> toNamedLogFile(file))
                                        .toList()));
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/log-analysis/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LogAnalysisJobStatus> startAnalyzeLogJob(@RequestParam("file") List<MultipartFile> files) {
        List<MultipartFile> nonEmptyFiles = files == null
                ? List.of()
                : files.stream().filter(file -> !file.isEmpty()).toList();
        if (nonEmptyFiles.isEmpty()) {
            throw new IllegalArgumentException("Ingen loggfil valdes.");
        }

        List<MultipartFile> sortedFiles = sortFiles(nonEmptyFiles);
        String sourceLog = sortedFiles.size() == 1
                ? sortedFiles.get(0).getOriginalFilename()
                : "Folder upload: " + inferFolderLabel(sortedFiles) + " (" + sortedFiles.size() + " filer)";

        LogAnalysisJobStatus status = logAnalysisJobService.startFileJob(
                sourceLog,
                sortedFiles.stream().map(this::toNamedLogFile).toList());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(status);
    }

    @GetMapping("/log-analysis/jobs/{jobId}")
    public LogAnalysisJobStatus getAnalyzeLogJob(@PathVariable String jobId) {
        return logAnalysisJobService.getJob(jobId);
    }

    @PostMapping(value = "/log-analysis/import-sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LogAnalysisImportSessionService.ImportSessionProgress startImportSession(
            @RequestBody ImportSessionStartRequest request) {
        int totalFiles = request.totalFiles() == null ? 0 : request.totalFiles();
        return logAnalysisImportSessionService.startSession(request.sourceLog(), totalFiles);
    }

    @PostMapping(value = "/log-analysis/import-sessions/{sessionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisImportSessionService.ImportSessionProgress appendImportSessionFile(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ingen loggfil valdes.");
        }

        return logAnalysisImportSessionService.appendFile(
                sessionId,
                sanitizeOriginalFilename(file.getOriginalFilename()),
                file.getBytes());
    }

    @PostMapping("/log-analysis/import-sessions/{sessionId}/complete")
    public LogAnalysisResult completeImportSession(@PathVariable String sessionId) throws IOException {
        return logAnalysisImportSessionService.completeSession(sessionId);
    }

    @PostMapping("/log-analysis/import-sessions/{sessionId}/complete-job")
    public ResponseEntity<LogAnalysisJobStatus> completeImportSessionAsJob(@PathVariable String sessionId) {
        LogAnalysisImportSessionService.CompletionRequest completionRequest =
                logAnalysisImportSessionService.prepareCompletion(sessionId);
        LogAnalysisJobStatus status = logAnalysisJobService.startScenarioJob(
                completionRequest.sourceLog(),
                completionRequest.scenario(),
                completionRequest.processedFiles());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(status);
    }

    private List<MultipartFile> sortFiles(List<MultipartFile> files) {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        return files.stream()
                .sorted(Comparator.comparing(
                        file -> sanitizeOriginalFilename(file.getOriginalFilename()),
                        collator))
                .toList();
    }

    private LogAnalysisService.NamedLogFile toNamedLogFile(MultipartFile file) {
        try {
            return new LogAnalysisService.NamedLogFile(
                    sanitizeOriginalFilename(file.getOriginalFilename()),
                    file.getBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Kunde inte läsa filen " + file.getOriginalFilename(), exception);
        }
    }

    private String inferFolderLabel(List<MultipartFile> files) {
        return files.stream()
                .map(file -> sanitizeOriginalFilename(file.getOriginalFilename()))
                .map(path -> path.contains("/") ? path.substring(0, path.indexOf('/')) : path)
                .filter(label -> !label.isBlank())
                .findFirst()
                .orElse("selected-folder");
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed.log";
        }
        return originalFilename.replace('\\', '/');
    }

    @GetMapping("/log-analysis/archive")
    public List<LogAnalysisSummary> listSavedAnalyses() throws IOException {
        return logAnalysisArchiveService.listSummaries();
    }

    @GetMapping("/log-analysis/archive/{analysisId}")
    public LogAnalysisResult getSavedAnalysis(@PathVariable String analysisId) throws IOException {
        return logAnalysisArchiveService.get(analysisId);
    }

    @PostMapping("/log-analysis/archive/{analysisId}/replay")
    public EvamScenarioReplayResult replaySavedAnalysis(@PathVariable String analysisId) throws IOException {
        LogAnalysisResult result = logAnalysisArchiveService.get(analysisId);
        return evamScenarioReplayService.replay(result.getScenario());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "Loggfilen är för stor. Nuvarande gräns är 64 MB."));
    }

    public record ImportSessionStartRequest(String sourceLog, Integer totalFiles) {
    }
}