package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;

import java.time.Duration;
import java.time.LocalDateTime;
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
            spent += (int) Duration.between(activeEntity.getStamp(), LocalDateTime.now()).getSeconds();
        }

        if(userIds == null || userIds.isEmpty()) {
            return timingRepository.sumTime(taskId, projectId) + spent;
        }
        return timingRepository.sumTime(taskId, projectId, userIds) + spent;
    }
}
