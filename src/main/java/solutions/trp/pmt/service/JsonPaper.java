package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import solutions.trp.jsonpaper.*;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class JsonPaper {
    private static final String DEVICE_FONT = "helvetica";

    private final ResourceLoader resourceLoader;
    private final TaskRepository taskRepository;
    private final TimingRepository timingRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public JsonPaper(
            ResourceLoader resourceLoader,
            TaskRepository taskRepository,
            TimingRepository timingRepository,
            ProjectRepository projectRepository) {
        this.resourceLoader = resourceLoader;
        this.taskRepository = taskRepository;
        this.timingRepository = timingRepository;
        this.projectRepository = projectRepository;
    }
    public String createDisplay() {
        try {
            List<TaskEntity> tasks = taskRepository.findAll();
            List<ProjectEntity> projects = projectRepository.findAll();

            int todoTaskCount = 0;
            int awaitingTaskCount = 0;
            int finishedTaskCount = 0;

            for (TaskEntity task : tasks) {
                switch (task.getStatus()){
                    case TODO -> todoTaskCount++;
                    case AWAITING_CONFIRMATION -> awaitingTaskCount++;
                    case FINISHED -> finishedTaskCount++;
                }
            }

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            List<LocalDate> workDays = workDaysFor(today);
            LocalDate thisWeekStart = today.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            LocalDate periodStart = thisWeekStart.minusWeeks(1);
            LocalDate periodEnd = thisWeekStart.plusWeeks(1);
            long[] workSeconds = getWorkSeconds(workDays, periodStart, periodEnd);
            int[] workHours = Arrays.stream(workSeconds)
                    .mapToInt(seconds -> (int) Math.round(seconds / 3600.0))
                    .toArray();
            String[] workDayLabels = workDays.stream()
                    .map(day -> day.getDayOfWeek().getDisplayName(
                            TextStyle.SHORT,
                            Locale.ENGLISH
                    ).toUpperCase(Locale.ENGLISH))
                    .toArray(String[]::new);
            Color[] workColors = workDays.stream()
                    .map(day -> day.isBefore(thisWeekStart)
                            ? Color.RED
                            : Color.YELLOW)
                    .toArray(Color[]::new);

            int totalTaskCount = todoTaskCount
                    + awaitingTaskCount
                    + finishedTaskCount;

            int todoSweepAngle = (int) (
                    360.0 * todoTaskCount / totalTaskCount
            );
            int awaitingSweepAngle = (int) (
                    360.0 * awaitingTaskCount / totalTaskCount
            );
            int finishedSweepAngle = (int) (
                    360.0 * finishedTaskCount / totalTaskCount
            );

            BufferedImage logo = loadLogo();

            Display display = new Display()
                    .clear(Color.WHITE)
                    .image(5, 5, logo, Color.TRANSPARENT)
                    .pieSlice(85, 185, 70, 0, todoSweepAngle, Color.RED)
                    .pieSlice(
                            85,
                            185,
                            70,
                            todoSweepAngle,
                            awaitingSweepAngle,
                            Color.YELLOW
                    )
                    .pieSlice(
                            85,
                            185,
                            70,
                            todoSweepAngle + awaitingSweepAngle,
                            finishedSweepAngle,
                            Color.BLACK
                    )
                    .circle(175, 157, 7, Color.RED, Width.W1, FillMode.FULL)
                    .text(label(195, 150, 100, 22, "Todo", 16, Color.RED))
                    .circle(175, 187, 7, Color.YELLOW, Width.W1, FillMode.FULL)
                    .text(label(195, 180, 100, 22, "Awaiting", 16, Color.YELLOW))
                    .circle(175, 217, 7, Color.BLACK, Width.W1, FillMode.FULL)
                    .text(label(195, 210, 100, 22, "Finished", 16, Color.BLACK));

            int chartLeft = 320;
            int chartTop = 10;
            int chartRight = 780;
            int chartBottom = 160;
            int chartInnerHeight = chartBottom - chartTop - 2;
            int barSlotWidth = (chartRight - chartLeft) / workHours.length;
            int barWidth = barSlotWidth - 4;
            int maxWorkHours = Math.max(1, Arrays.stream(workHours).max().orElse(1));

            display.rectangle(
                    chartLeft,
                    chartTop,
                    chartRight,
                    chartBottom,
                    Color.BLACK,
                    Width.W1,
                    FillMode.EMPTY
            );

            int tickStep = Math.max(1, (int) Math.ceil(maxWorkHours / 6.0));
            for (int hour = tickStep; hour <= maxWorkHours; hour += tickStep) {
                addTick(
                        display,
                        hour,
                        maxWorkHours,
                        chartLeft,
                        chartTop,
                        chartRight,
                        chartBottom,
                        chartInnerHeight
                );
            }
            if (maxWorkHours % tickStep != 0) {
                addTick(
                        display,
                        maxWorkHours,
                        maxWorkHours,
                        chartLeft,
                        chartTop,
                        chartRight,
                        chartBottom,
                        chartInnerHeight
                );
            }

            for (int barIndex = 0; barIndex < workHours.length; barIndex++) {
                int hours = workHours[barIndex];
                int barCenter = chartLeft
                        + barIndex * barSlotWidth
                        + barSlotWidth / 2;
                int barHeight = (int) Math.round(
                        (double) chartInnerHeight * hours / maxWorkHours
                );

                if (barHeight > 0) {
                    display.rectangle(
                            barCenter - barWidth / 2,
                            chartBottom - barHeight,
                            barCenter + barWidth / 2 - 1,
                            chartBottom,
                            workColors[barIndex],
                            Width.W1,
                            FillMode.FULL
                    );
                }

                display.text(label(
                        barCenter - 20,
                        165,
                        40,
                        16,
                        workDayLabels[barIndex],
                        12,
                        Color.BLACK
                ));
            }

            long totalWorkSeconds = timingRepository.sumTime(null, null);
            long totalWorkHours = Math.round(totalWorkSeconds / 3600.0);
            display
                    .text(label(
                            320,
                            210,
                            300,
                            22,
                            "Total hours spend: " + totalWorkHours,
                            16,
                            Color.BLACK
                    ))
                    .text(label(
                            320,
                            240,
                            300,
                            22,
                            "Total Projects: " + projects.size(),
                            16,
                            Color.BLACK
                    ));

            return display.toJson();
        } catch (Exception e){ return ""; }
    }

    private long[] getWorkSeconds(
            List<LocalDate> workDays,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        Instant rangeStart = periodStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = periodEnd.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<TimingEntity> timings =
                timingRepository.findAllByEndTimeAfterAndStartTimeBefore(
                        Timestamp.from(rangeStart),
                        Timestamp.from(rangeEnd)
                );
        long[] secondsPerDay = new long[workDays.size()];

        for (int dayIndex = 0; dayIndex < workDays.size(); dayIndex++) {
            Instant dayStart = workDays.get(dayIndex)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            Instant dayEnd = dayStart.plus(Duration.ofDays(1));

            for (TimingEntity timing : timings) {
                Instant overlapStart = timing.getStartTime().toInstant()
                        .isAfter(dayStart)
                        ? timing.getStartTime().toInstant()
                        : dayStart;
                Instant overlapEnd = timing.getEndTime().toInstant().isBefore(dayEnd)
                        ? timing.getEndTime().toInstant()
                        : dayEnd;

                if (overlapStart.isBefore(overlapEnd)) {
                    secondsPerDay[dayIndex] +=
                            Duration.between(overlapStart, overlapEnd).getSeconds();
                }
            }
        }
        return secondsPerDay;
    }

    private static boolean isWeekday(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    static List<LocalDate> workDaysFor(LocalDate today) {
        LocalDate currentWeekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate lastWeekStart = currentWeekStart.minusWeeks(1);
        List<LocalDate> workDays = new ArrayList<>();

        workDays.addAll(lastWeekStart.datesUntil(currentWeekStart)
                .filter(JsonPaper::isWeekday)
                .toList());
        workDays.addAll(currentWeekStart.datesUntil(currentWeekStart.plusWeeks(1))
                .filter(JsonPaper::isWeekday)
                .toList());

        return List.copyOf(workDays);
    }

    private BufferedImage loadLogo() throws IOException {
        Resource resource = resourceLoader.getResource(
                "classpath:frontend/resource/logo.svg"
        );
        Path temporaryLogo = Files.createTempFile("pmt-logo-", ".svg");

        try (InputStream input = resource.getInputStream()) {
            Files.copy(input, temporaryLogo, StandardCopyOption.REPLACE_EXISTING);
            return ImageLoader.load(temporaryLogo, 200, 50);
        } finally {
            Files.deleteIfExists(temporaryLogo);
        }
    }

    private static void addTick(
            Display display,
            int hour,
            int maxWorkHours,
            int chartLeft,
            int chartTop,
            int chartRight,
            int chartBottom,
            int chartInnerHeight
    ) {
        int tickY = chartBottom - (int) Math.round(
                (double) chartInnerHeight * hour / maxWorkHours
        );
        String tickText = Integer.toString(hour);

        display.text(TextBox.builder(
                        chartLeft - 40,
                        Math.max(chartTop, Math.min(chartBottom - 12, tickY - 6)),
                        34,
                        16
                )
                .horizontalAlign(HorizontalAlign.RIGHT)
                .span(TextSpan.builder(tickText)
                        .family(DEVICE_FONT)
                        .size(12)
                        .build())
                .build());

        if (hour != maxWorkHours) {
            display.line(
                    chartLeft,
                    tickY,
                    chartRight,
                    tickY,
                    Color.BLACK,
                    Width.W1,
                    LineStyle.DOTTED
            );
        }
    }

    private static TextBox label(
            int x,
            int y,
            int width,
            int height,
            String text,
            int size,
            Color color
    ) {
        return new TextBox(x, y, width, height, List.of(
                TextSpan.builder(text)
                        .family(DEVICE_FONT)
                        .size(size)
                        .color(color)
                        .build()
        ));
    }
}
