package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisJobStatus;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Service
public class LogAnalysisJobService implements DisposableBean {

    private final LogAnalysisService logAnalysisService;
    private final LogAnalysisArchiveService logAnalysisArchiveService;
    private final Map<String, AnalysisJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new NamedThreadFactory());

    public LogAnalysisJobService(
            LogAnalysisService logAnalysisService,
            LogAnalysisArchiveService logAnalysisArchiveService) {
        this.logAnalysisService = logAnalysisService;
        this.logAnalysisArchiveService = logAnalysisArchiveService;
    }

    public LogAnalysisJobStatus startFileJob(String sourceLog, List<LogAnalysisService.NamedLogFile> logFiles) {
        if (logFiles == null || logFiles.isEmpty()) {
            throw new IllegalArgumentException("Ingen loggfil valdes.");
        }

        AnalysisJob job = createJob(logFiles.size(), sourceLog);
        executorService.submit(() -> runFileJob(job, sourceLog, logFiles));
        return snapshot(job);
    }

    public LogAnalysisJobStatus startScenarioJob(String sourceLog, EvamLogScenario scenario, int totalFiles) {
        if (scenario == null) {
            throw new IllegalArgumentException("Ingen importsession finns att slutföra.");
        }

        AnalysisJob job = createJob(Math.max(totalFiles, 1), sourceLog);
        executorService.submit(() -> runScenarioJob(job, scenario));
        return snapshot(job);
    }

    public LogAnalysisJobStatus getJob(String jobId) {
        AnalysisJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Ingen analys hittades för jobb-id " + jobId);
        }
        return snapshot(job);
    }

    private AnalysisJob createJob(int totalFiles, String sourceLog) {
        String jobId = UUID.randomUUID().toString();
        AnalysisJob job = new AnalysisJob(jobId, totalFiles, sourceLog);
        jobs.put(jobId, job);
        return job;
    }

    private void runFileJob(AnalysisJob job, String sourceLog, List<LogAnalysisService.NamedLogFile> logFiles) {
        try {
            update(job, LogAnalysisJobStatus.JobState.RUNNING, "queued", "Analysen är startad.", 0, job.totalFiles, null);

            LogAnalysisResult result = logFiles.size() == 1
                    ? logAnalysisService.analyze(sourceLog, logFiles.get(0).content(), progress -> applyProgress(job, progress))
                    : logAnalysisService.analyzeMany(sourceLog, logFiles, progress -> applyProgress(job, progress));

            update(job, LogAnalysisJobStatus.JobState.RUNNING, "saving", "Sparar analys och scenarios...", job.totalFiles, job.totalFiles, null);
            LogAnalysisResult saved = logAnalysisArchiveService.save(result);
            complete(job, saved);
        } catch (Exception exception) {
            fail(job, exception);
        }
    }

    private void runScenarioJob(AnalysisJob job, EvamLogScenario scenario) {
        try {
            update(job, LogAnalysisJobStatus.JobState.RUNNING, "analyzing", "Bygger API-sekvens från importerade filer...", job.totalFiles, job.totalFiles, null);
            LogAnalysisResult result = logAnalysisService.analyzeScenario(scenario, progress -> applyProgress(job, progress));
            update(job, LogAnalysisJobStatus.JobState.RUNNING, "saving", "Sparar analys och scenarios...", job.totalFiles, job.totalFiles, null);
            LogAnalysisResult saved = logAnalysisArchiveService.save(result);
            complete(job, saved);
        } catch (Exception exception) {
            fail(job, exception);
        }
    }

    private void applyProgress(AnalysisJob job, LogAnalysisService.ProgressUpdate progress) {
        int processedFiles = progress.processedFiles() == null ? job.processedFiles : progress.processedFiles();
        int totalFiles = progress.totalFiles() == null ? job.totalFiles : progress.totalFiles();
        update(job, LogAnalysisJobStatus.JobState.RUNNING, progress.phase(), progress.message(), processedFiles, totalFiles, progress.currentFile());
    }

    private void complete(AnalysisJob job, LogAnalysisResult result) {
        synchronized (job) {
            job.state = LogAnalysisJobStatus.JobState.COMPLETED;
            job.phase = "completed";
            job.message = "Analysen är klar.";
            job.processedFiles = job.totalFiles;
            job.currentFile = null;
            job.result = result;
            job.errorMessage = null;
        }
    }

    private void fail(AnalysisJob job, Exception exception) {
        synchronized (job) {
            job.state = LogAnalysisJobStatus.JobState.FAILED;
            job.phase = "failed";
            job.message = "Analysen misslyckades.";
            job.currentFile = null;
            job.errorMessage = exception.getMessage();
        }
    }

    private void update(
            AnalysisJob job,
            LogAnalysisJobStatus.JobState state,
            String phase,
            String message,
            int processedFiles,
            int totalFiles,
            String currentFile) {
        synchronized (job) {
            job.state = state;
            job.phase = phase;
            job.message = message;
            job.processedFiles = processedFiles;
            job.totalFiles = totalFiles;
            job.currentFile = currentFile;
        }
    }

    private LogAnalysisJobStatus snapshot(AnalysisJob job) {
        synchronized (job) {
            return new LogAnalysisJobStatus(
                    job.jobId,
                    job.state,
                    job.phase,
                    job.message,
                    job.processedFiles,
                    job.totalFiles,
                    job.currentFile,
                    job.result,
                    job.errorMessage);
        }
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

    private static final class AnalysisJob {
        private final String jobId;
        private final String sourceLog;
        private LogAnalysisJobStatus.JobState state;
        private String phase;
        private String message;
        private int processedFiles;
        private int totalFiles;
        private String currentFile;
        private LogAnalysisResult result;
        private String errorMessage;

        private AnalysisJob(String jobId, int totalFiles, String sourceLog) {
            this.jobId = jobId;
            this.sourceLog = sourceLog;
            this.state = LogAnalysisJobStatus.JobState.QUEUED;
            this.phase = "queued";
            this.message = "Analysen väntar på att starta.";
            this.processedFiles = 0;
            this.totalFiles = totalFiles;
            this.currentFile = null;
            this.result = null;
            this.errorMessage = null;
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "log-analysis-job-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}