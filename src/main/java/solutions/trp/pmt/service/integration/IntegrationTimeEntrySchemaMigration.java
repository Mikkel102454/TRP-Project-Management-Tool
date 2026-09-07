package solutions.trp.pmt.service.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Backfills the account snapshot for entries created before remote time editing was supported. */
@Component
public class IntegrationTimeEntrySchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public IntegrationTimeEntrySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("""
                UPDATE integration_time_entry time_entry
                INNER JOIN integration_user_binding binding
                    ON binding.user_id = time_entry.user_id
                    AND binding.provider = time_entry.provider
                SET time_entry.remote_account_id = binding.remote_account_id
                WHERE time_entry.remote_account_id IS NULL
                """);
    }
}
