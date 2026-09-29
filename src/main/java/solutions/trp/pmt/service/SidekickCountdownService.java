package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.datasource.gadget.SidekickCountdownEntity;
import solutions.trp.pmt.datasource.gadget.SidekickCountdownRepository;

import java.time.Duration;
import java.time.Instant;

@Service
public class SidekickCountdownService {
    private final SidekickCountdownRepository repository;

    public SidekickCountdownService(SidekickCountdownRepository repository) {
        this.repository = repository;
    }

    public synchronized void addTime(int userId) {
        addTime(userId, Instant.now());
    }

    void addTime(int userId, Instant now) {
        SidekickCountdownEntity countdown = repository.findById(userId).orElseGet(SidekickCountdownEntity::new);
        countdown.setUserId(userId);
        if (countdown.getEndTime() == null || !countdown.getEndTime().isAfter(now)) {
            countdown.setAnchorTime(now);
            countdown.setEndTime(now);
        }
        countdown.setEndTime(countdown.getEndTime().plusSeconds(15 * 60));
        repository.saveAndFlush(countdown);
    }

    public synchronized void clear(int userId) {
        repository.findById(userId).ifPresent(repository::delete);
    }

    public synchronized SidekickService.Overlay overlay(int userId) {
        return overlay(userId, Instant.now());
    }

    SidekickService.Overlay overlay(int userId, Instant now) {
        return repository.findById(userId).filter(timer -> timer.getEndTime().isAfter(now))
                .map(timer -> new SidekickService.Overlay("timer", "non-project-timer", "down", "running",
                        timer.getAnchorTime().toString(), Duration.between(timer.getAnchorTime(), timer.getEndTime()).getSeconds(),
                        "hh:mm:ss", "center", 24, "#FFFFFF", "#00000080"))
                .orElse(null);
    }
}
