package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LogAnalysisImportSessionService {

    private final LogAnalysisService logAnalysisService;
    private final LogAnalysisArchiveService logAnalysisArchiveService;
    private final Map<String, ImportSession> sessions = new ConcurrentHashMap<>();

    public LogAnalysisImportSessionService(
            LogAnalysisService logAnalysisService,
            LogAnalysisArchiveService logAnalysisArchiveService) {
        this.logAnalysisService = logAnalysisService;
        this.logAnalysisArchiveService = logAnalysisArchiveService;
    }

    public ImportSessionProgress startSession(String sourceLog, int totalFiles) {
        if (sourceLog == null || sourceLog.isBlank()) {
            throw new IllegalArgumentException("Källnamn för importen saknas.");
        }
        if (totalFiles < 1) {
            throw new IllegalArgumentException("Minst en fil krävs för stegvis import.");
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new ImportSession(
                sessionId,
                sourceLog,
                totalFiles,
                0,
                null,
                logAnalysisService.createScenarioShell(sourceLog)));
        return toProgress(sessions.get(sessionId));
    }

    public ImportSessionProgress appendFile(String sessionId, String filePath, byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Filen är tom och kunde inte importeras.");
        }

        ImportSession session = getRequiredSession(sessionId);
        synchronized (session) {
            session.scenario = logAnalysisService.appendToScenario(
                    session.scenario,
                    new LogAnalysisService.NamedLogFile(filePath, content));
            session.processedFiles += 1;
            session.lastFile = filePath;
            return toProgress(session);
        }
    }

    public LogAnalysisResult completeSession(String sessionId) throws IOException {
        ImportSession session = getRequiredSession(sessionId);
        synchronized (session) {
            if (session.processedFiles < 1) {
                throw new IllegalStateException("Importsessionen innehåller inga uppladdade filer.");
            }

            LogAnalysisResult result = logAnalysisArchiveService.save(
                    logAnalysisService.analyzeScenario(session.scenario));
            sessions.remove(sessionId);
            return result;
        }
    }

    public CompletionRequest prepareCompletion(String sessionId) {
        ImportSession session = getRequiredSession(sessionId);
        synchronized (session) {
            if (session.processedFiles < 1) {
                throw new IllegalStateException("Importsessionen innehåller inga uppladdade filer.");
            }

            sessions.remove(sessionId);
            return new CompletionRequest(
                    session.sourceLog,
                    session.processedFiles,
                    session.scenario);
        }
    }

    private ImportSession getRequiredSession(String sessionId) {
        ImportSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Ingen aktiv importsession hittades för id " + sessionId);
        }
        return session;
    }

    private ImportSessionProgress toProgress(ImportSession session) {
        return new ImportSessionProgress(
                session.sessionId,
                session.sourceLog,
                session.processedFiles,
                session.totalFiles,
                session.lastFile);
    }

    public record ImportSessionProgress(
            String sessionId,
            String sourceLog,
            int processedFiles,
            int totalFiles,
            String lastFile) {
    }

        public record CompletionRequest(
            String sourceLog,
            int processedFiles,
            EvamLogScenario scenario) {
        }

    private static final class ImportSession {
        private final String sessionId;
        private final String sourceLog;
        private final int totalFiles;
        private int processedFiles;
        private String lastFile;
        private EvamLogScenario scenario;

        private ImportSession(
                String sessionId,
                String sourceLog,
                int totalFiles,
                int processedFiles,
                String lastFile,
                EvamLogScenario scenario) {
            this.sessionId = sessionId;
            this.sourceLog = sourceLog;
            this.totalFiles = totalFiles;
            this.processedFiles = processedFiles;
            this.lastFile = lastFile;
            this.scenario = scenario;
        }
    }
}