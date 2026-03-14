package com.dedalus.amphi_integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dedalus.amphi_integration.model.OperationDistance;

class OperationDistanceRepositoryOrderingTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadFromDiskNormalizesOperationDistancesToTimestampOrder() throws IOException {
        Path dataDirectory = tempDir.resolve("data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("OperationDistance.json"), """
                {
                  "later": {
                    "timestamp": "2026-03-07T17:58:03",
                    "operationID": "op-1",
                    "distance": 20.0,
                    "assignmentDistance": 20.0,
                    "publishedAssignmentDistance": 20.0,
                    "stateID": "2",
                    "stateEntryDistance": 20.0
                  },
                  "earlier": {
                    "timestamp": "2026-03-07T17:57:17",
                    "operationID": "op-1",
                    "distance": 0.0,
                    "assignmentDistance": 0.0,
                    "publishedAssignmentDistance": 0.0,
                    "stateID": "1",
                    "stateEntryDistance": 0.0
                  },
                  "middle": {
                    "timestamp": "2026-03-07T17:57:23",
                    "operationID": "op-1",
                    "distance": 10.0,
                    "assignmentDistance": 10.0,
                    "publishedAssignmentDistance": 0.0,
                    "stateID": "1",
                    "stateEntryDistance": 10.0
                  }
                }
                """);

        TestOperationDistanceRepository repository = new TestOperationDistanceRepository(dataDirectory);
        repository.init();
        repository.reloadFromDisk();

        List<OperationDistance> loadedDistances = repository.findAll();

        assertThat(loadedDistances)
                .extracting(OperationDistance::getTimestamp)
                .containsExactly(
                        LocalDateTime.parse("2026-03-07T17:57:17"),
                        LocalDateTime.parse("2026-03-07T17:57:23"),
                        LocalDateTime.parse("2026-03-07T17:58:03"));

        repository.save(OperationDistance.builder()
                .timestamp(LocalDateTime.parse("2026-03-07T17:58:21"))
                .operationID("op-1")
                .distance(30.0)
                .assignmentDistance(30.0)
                .publishedAssignmentDistance(30.0)
                .stateID("3")
                .stateEntryDistance(30.0)
                .build());

        String persistedJson = Files.readString(dataDirectory.resolve("OperationDistance.json"));

        assertThat(persistedJson.indexOf("2026-03-07T17:57:17")).isLessThan(persistedJson.indexOf("2026-03-07T17:57:23"));
        assertThat(persistedJson.indexOf("2026-03-07T17:57:23")).isLessThan(persistedJson.indexOf("2026-03-07T17:58:03"));
        assertThat(persistedJson.indexOf("2026-03-07T17:58:03")).isLessThan(persistedJson.indexOf("2026-03-07T17:58:21"));
    }

    private static final class TestOperationDistanceRepository extends OperationDistanceRepository {
        private final Path dataDirectoryPath;

        private TestOperationDistanceRepository(Path dataDirectoryPath) {
            this.dataDirectoryPath = dataDirectoryPath;
        }

        @Override
        protected Path getDataDirectoryPath() {
            return dataDirectoryPath;
        }
    }
}