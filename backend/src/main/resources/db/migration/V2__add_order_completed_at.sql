-- V2__add_order_completed_at.sql
ALTER TABLE orders ADD COLUMN completed_at DATETIME(6);
