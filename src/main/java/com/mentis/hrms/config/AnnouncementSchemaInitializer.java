package com.mentis.hrms.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementSchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AnnouncementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureAnnouncementSchema() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS announcements (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "title VARCHAR(255) NOT NULL," +
                            "description TEXT NOT NULL," +
                            "type ENUM('PERMANENT','TEMPORARY') NOT NULL," +
                            "announcement_type VARCHAR(20) NOT NULL," +
                            "expiry_date DATETIME NULL," +
                            "category VARCHAR(100)," +
                            "priority ENUM('LOW','NORMAL','HIGH','URGENT') NOT NULL DEFAULT 'NORMAL'," +
                            "target_audience ENUM('ALL','HR_ONLY','EMPLOYEES_ONLY') NOT NULL DEFAULT 'ALL'," +
                            "created_by VARCHAR(100)," +
                            "created_by_name VARCHAR(255)," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "expires_at DATETIME NULL," +
                            "is_active BOOLEAN NOT NULL DEFAULT TRUE," +
                            "is_pinned BOOLEAN NOT NULL DEFAULT FALSE" +
                            ")"
            );

            ensureColumn("description", "ALTER TABLE announcements ADD COLUMN description TEXT NULL");
            ensureColumn("announcement_type", "ALTER TABLE announcements ADD COLUMN announcement_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL'");
            ensureColumn("expiry_date", "ALTER TABLE announcements ADD COLUMN expiry_date DATETIME NULL");
            ensureColumn("category", "ALTER TABLE announcements ADD COLUMN category VARCHAR(100)");
            ensureColumn("priority", "ALTER TABLE announcements ADD COLUMN priority ENUM('LOW','NORMAL','HIGH','URGENT') NOT NULL DEFAULT 'NORMAL'");
            ensureColumn("target_audience", "ALTER TABLE announcements ADD COLUMN target_audience ENUM('ALL','HR_ONLY','EMPLOYEES_ONLY') NOT NULL DEFAULT 'ALL'");
            ensureColumn("created_by", "ALTER TABLE announcements ADD COLUMN created_by VARCHAR(100)");
            ensureColumn("created_by_name", "ALTER TABLE announcements ADD COLUMN created_by_name VARCHAR(255)");
            ensureColumn("created_at", "ALTER TABLE announcements ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn("expires_at", "ALTER TABLE announcements ADD COLUMN expires_at DATETIME NULL");
            ensureColumn("is_active", "ALTER TABLE announcements ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE");
            ensureColumn("is_pinned", "ALTER TABLE announcements ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE");

            // Backfill description from legacy content column if needed.
            if (columnExists("content")) {
                jdbcTemplate.execute("UPDATE announcements SET description = content WHERE description IS NULL");
            }
            jdbcTemplate.execute("UPDATE announcements SET announcement_type = 'GENERAL' WHERE announcement_type IS NULL OR announcement_type = ''");

            logger.info("Announcement schema verified successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize announcements schema: {}", e.getMessage(), e);
        }
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            logger.info("Added missing announcements column: {}", columnName);
        }
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }
}
