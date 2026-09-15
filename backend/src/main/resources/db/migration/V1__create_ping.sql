-- One table so the migration path is exercised end to end. No domain meaning.
CREATE TABLE ping (
    id   uuid        NOT NULL PRIMARY KEY,
    note varchar(50) NOT NULL DEFAULT 'ok'
);
