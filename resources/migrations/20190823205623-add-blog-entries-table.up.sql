CREATE TABLE blog_entries
(id UUID PRIMARY KEY,
 created TIMESTAMP,
 last_modified TIMESTAMP,
 author_id UUID REFERENCES users(id),
 header VARCHAR(120),
 summary TEXT,
 content TEXT);