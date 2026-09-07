package solutions.trp.pmt.service.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IntegrationActiveSchemaMigration implements ApplicationRunner {
    private static final String TABLE = "integration_active_timer";
    private static final String OLD_INDEX = "uk_integration_active_user_provider";
    private static final String NEW_INDEX = "uk_integration_active_user_task";

    private final JdbcTemplate jdbcTemplate;

    public IntegrationActiveSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!indexExists(NEW_INDEX)) {
            jdbcTemplate.execute("ALTER TABLE " + TABLE
                    + " ADD CONSTRAINT " + NEW_INDEX + " UNIQUE (user_id, task_ref)");
        }
        if (indexExists(OLD_INDEX)) {
            jdbcTemplate.execute("ALTER TABLE " + TABLE + " DROP INDEX " + OLD_INDEX);
        }
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, Integer.class, TABLE, indexName);
        return count != null && count > 0;
    }
}
