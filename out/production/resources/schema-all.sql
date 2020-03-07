
drop table legacydocument if exists;
Create Table legacydocument (
    doc_id BIGINT IDENTITY NOT NULL PRIMARY KEY,
    doclocator VARCHAR(50)
);

drop table if exists result;
create table result
(
    id     BIGINT GENERATED ALWAYS AS IDENTITY,
    result int,
    note   varchar(50)
);