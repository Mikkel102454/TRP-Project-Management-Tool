package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class TimeService {
    private final TimingRepository timingRepository;
    private final ActiveRepository activeRepository;

    public TimeService(TimingRepository timingRepository, ActiveRepository activeRepository) {
        this.timingRepository = timingRepository;
        this.activeRepository = activeRepository;
    }

    public int calculateTime(Integer taskId, Integer projectId, List<Integer> userIds) {


        int spent = 0;
        for(ActiveEntity activeEntity : activeRepository.findAllByTaskEntity_Id(taskId)) {
            spent += (int) Duration.between(
                    activeEntity.getStamp().toInstant(),
                    Instant.now()
            ).getSeconds();
        }

        if(userIds == null || userIds.isEmpty()) {
            return timingRepository.sumTime(taskId, projectId) + spent;
        }
        return timingRepository.sumTime(taskId, projectId, userIds) + spent;
    }

    public List<TimingEntity> getAllTime() {
        return timingRepository.findAll();
    }

    public List<TimingEntity> getAllTimeByUserId(int userId) {
        return timingRepository.findAllByUserEntity_Id(userId);
    }

    public void deleteTimeEntry(int id) {
        timingRepository.deleteById(id);
    }

    public void updateTimeEntry(int id, OffsetDateTime startTime, OffsetDateTime endTime) {
        TimingEntity timingEntity = timingRepository.findById(id).orElseThrow(() -> new NotFoundException("Could not find time entry"));
        timingEntity.setStartTime(Timestamp.from(startTime.toInstant()));
        timingEntity.setEndTime(Timestamp.from(endTime.toInstant()));

        timingRepository.save(timingEntity);
    }
}
