-- Runs once on first startup of the postgres container (empty data volume).
-- One database per DB-backed service; each service's Flyway migrations create
-- and own the schema inside its database.
CREATE DATABASE authdb;
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
