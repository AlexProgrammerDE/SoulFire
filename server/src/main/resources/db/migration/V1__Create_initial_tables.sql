-- V1__Create_initial_tables.sql
CREATE TABLE users (
                     id SERIAL PRIMARY KEY,
                     username VARCHAR(50) NOT NULL,
                     password VARCHAR(50) NOT NULL
);
