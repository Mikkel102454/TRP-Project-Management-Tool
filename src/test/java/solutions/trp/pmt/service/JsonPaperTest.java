package solutions.trp.pmt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonPaperTest {
    @Test
    void workDaysPutLastWeekBeforeThisWeek() {
        List<LocalDate> workDays = JsonPaper.workDaysFor(
                LocalDate.of(2026, 7, 30)
        );

        assertEquals(
                List.of(
                        LocalDate.of(2026, 7, 20),
                        LocalDate.of(2026, 7, 21),
                        LocalDate.of(2026, 7, 22),
                        LocalDate.of(2026, 7, 23),
                        LocalDate.of(2026, 7, 24),
                        LocalDate.of(2026, 7, 27),
                        LocalDate.of(2026, 7, 28),
                        LocalDate.of(2026, 7, 29),
                        LocalDate.of(2026, 7, 30),
                        LocalDate.of(2026, 7, 31)
                ),
                workDays
        );
    }

    @Test
    void displayUsesJsonPaperVersionTwoTextCommands() throws Exception {
        TaskRepository tasks = mock(TaskRepository.class);
        TimingRepository timings = mock(TimingRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);

        TaskEntity todo = taskWithStatus(TaskEntity.TaskStatus.TODO);
        TaskEntity awaiting = taskWithStatus(TaskEntity.TaskStatus.AWAITING_CONFIRMATION);
        TaskEntity finished = taskWithStatus(TaskEntity.TaskStatus.FINISHED);

        when(tasks.findAll()).thenReturn(List.of(todo, awaiting, finished));
        when(timings.findAllByEndTimeAfterAndStartTimeBefore(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());
        when(timings.sumTime(null, null)).thenReturn(0L);
        when(projects.findAll()).thenReturn(List.of(mock(ProjectEntity.class)));

        JsonPaper jsonPaper = new JsonPaper(
                new DefaultResourceLoader(),
                tasks,
                timings,
                projects
        );
        JsonNode document = new ObjectMapper().readTree(jsonPaper.createDisplay());

        assertEquals("2.0", document.path("version").asText());
        assertTrue(document.path("commands").isArray());
        assertFalse(hasCommand(document, "draw_string"));
        assertTrue(hasCommand(document, "draw_text"));

        for (JsonNode command : document.path("commands")) {
            if (!"draw_text".equals(command.path("cmd").asText())) {
                continue;
            }

            JsonNode arguments = command.path("args");
            assertTrue(arguments.path("width").asInt() > 0);
            assertTrue(arguments.path("height").asInt() > 0);
            assertTrue(arguments.path("spans").isArray());
            assertFalse(arguments.path("spans").isEmpty());

            for (JsonNode span : arguments.path("spans")) {
                assertEquals("helvetica", span.path("family").asText());
                assertTrue(span.path("size").asInt() == 12
                        || span.path("size").asInt() == 16);
                assertFalse(span.path("color").asText().isBlank());
            }
        }
    }

    private static TaskEntity taskWithStatus(TaskEntity.TaskStatus status) {
        TaskEntity task = new TaskEntity();
        task.setStatus(status);
        return task;
    }

    private static boolean hasCommand(JsonNode document, String commandName) {
        for (JsonNode command : document.path("commands")) {
            if (commandName.equals(command.path("cmd").asText())) {
                return true;
            }
        }
        return false;
    }
}
