CREATE TABLE blog_entries
(id VARCHAR(20) PRIMARY KEY,
 created TIMESTAMP,
 last_modified TIMESTAMP,
 author_id VARCHAR(20),
 header VARCHAR(120),
 summary TEXT,
 content TEXT);