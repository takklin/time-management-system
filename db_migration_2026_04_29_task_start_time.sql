-- Migration: Add start_time column to task, copy from deadline, then drop deadline
-- 1) Add new column
ALTER TABLE `task` ADD COLUMN `start_time` DATETIME DEFAULT NULL;

-- 2) Copy existing values
UPDATE `task` SET `start_time` = `deadline` WHERE `deadline` IS NOT NULL;

-- 3) (Optional) drop old column. Uncomment to remove the legacy column after verification.
-- ALTER TABLE `task` DROP COLUMN `deadline`;

-- Note: run this migration in a safe maintenance window and ensure backups exist before dropping `deadline`.
