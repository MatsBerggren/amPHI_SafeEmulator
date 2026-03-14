package com.dedalus.amphi_integration.model.loganalyzer;

public record LogAnalysisJobStatus(
        String jobId,
        JobState state,
        String phase,
        String message,
        int processedFiles,
        int totalFiles,
        String currentFile,
        LogAnalysisResult result,
        String errorMessage) {

    public enum JobState {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }
}