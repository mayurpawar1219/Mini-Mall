-- V6__update_pickup_slots.sql
ALTER TABLE pickup_slots
ADD COLUMN `date` DATE NOT NULL AFTER id,
ADD COLUMN `enabled` BOOLEAN NOT NULL DEFAULT TRUE AFTER capacity,
ADD COLUMN `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER current_bookings,
ADD COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
ADD CONSTRAINT uk_pickup_slots_date_time UNIQUE (`date`, `start_time`);
