CREATE TABLE blog_entries
(id UUID PRIMARY KEY,
 created TIMESTAMP,
 last_modified TIMESTAMP,
 author VARCHAR(20) REFERENCES users(username),
 header VARCHAR(120),
 summary TEXT,
 content TEXT);