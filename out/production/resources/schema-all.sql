DROP TABLE people IF EXISTS;

CREATE TABLE people  (
    person_id BIGINT IDENTITY NOT NULL PRIMARY KEY,
    first_name VARCHAR(20),
    last_name VARCHAR(20)
);

Create Table legacydocument (
    doc_id BIGINT IDENTITY NOT NULL PRIMARY KEY,
    doc_locator VARCHAR(50)
);
