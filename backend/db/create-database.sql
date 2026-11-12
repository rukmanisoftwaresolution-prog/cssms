-- Run once in pgAdmin: PostgreSQL 18 > right-click "postgres" database > Query Tool > paste > F5.
-- Creates the application role and the empty "cssms" database. All tables are then created
-- automatically by Flyway when the backend starts (db/migration/V1, V2).

CREATE ROLE cssms WITH LOGIN PASSWORD 'cssms';   -- change the password for anything but local development

CREATE DATABASE cssms
    OWNER cssms
    ENCODING 'UTF8';
