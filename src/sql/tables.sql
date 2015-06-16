-- name: create-table-msg!
CREATE TABLE IF NOT EXISTS msg(
       id IDENTITY PRIMARY KEY,
       content VARCHAR(512) NOT NULL,
       sender VARCHAR(32) NOT NULL,
       target VARCHAR(64) NOT NULL,
       date DATE NOT NULL,
       time TIME NOT NULL);

-- name: create-table-url!
CREATE TABLE IF NOT EXISTS url(
       id IDENTITY PRIMARY KEY,
       address VARCHAR(512) NOT NULL UNIQUE,
       title VARCHAR(512) NOT NULL);

-- name: create-table-url-msg!
CREATE TABLE IF NOT EXISTS url_msg(
       id IDENTITY PRIMARY KEY,
       urlid BIGINT NOT NULL REFERENCES url(id),
       msgid BIGINT NOT NULL REFERENCES msg(id));
