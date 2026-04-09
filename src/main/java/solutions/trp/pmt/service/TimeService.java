package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;

import java.util.List;

@Service
public class TimeService {
    private final TimingRepository timingRepository;

    public TimeService(TimingRepository timingRepository) {
        this.timingRepository = timingRepository;
    }

    public long calculateTime(Integer taskId, Integer projectId, List<Integer> userIds) {
        return timingRepository.sumTime(taskId, projectId, userIds);
    }
}
