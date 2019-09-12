
-- USERS

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(username, first_name, last_name, pass)
VALUES (:username, :first_name, :last_name, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, pass = :pass
WHERE username = :username

-- :name get-user :? :1
-- :doc retrieves a user record with given name
SELECT * FROM users
WHERE username = :username

-- :name get-users :? :*
-- :doc retrieves a user record given the id
SELECT * FROM users

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE username = :username

-- BLOG-ENTRIES

-- :name create-entry! :! :n
-- :doc creates a new blog entry
INSERT INTO blog_entries
(id, created, last_modified, author, header, summary, content)
VALUES (:id, :created, :last_modified, :author, :header, :summary, :content)

-- :name update-entry! :! :n
-- :doc updates an existing blog entry
UPDATE blog_entries
SET header = :header, summary = :summary, content = :content
WHERE id = :id

-- :name get-entries :? :*
-- :doc gets all blog entries
SELECT blog_entries.id, created, last_modified, author, header, summary, content, users.first_name, users.last_name
FROM blog_entries
INNER JOIN users ON users.username = blog_entries.author

-- :name get-entry :? :1
-- :doc gets single entry
SELECT blog_entries.id, created, last_modified, author, header, summary, content, users.first_name, users.last_name
FROM blog_entries
INNER JOIN users ON users.username = blog_entries.author
WHERE blog_entries.id = :id

-- :name delete-entry! :! :n
-- :doc deletes blog entry with given id
DELETE FROM blog_entries
WHERE id = :id

-- PAGE CONTENT

-- :name create-content! :! :n
-- :doc creates a new content
INSERT INTO page_content
(id, content)
VALUES (:id, :content)

-- :name update-content! :! :n
-- :doc updates an existing content
UPDATE page_content
SET content = :content
WHERE id = :id

-- :name get-content :? :1
-- :doc gets content by id
SELECT content
FROM page_content
WHERE id = :id

-- IMAGES

-- :name upload-image! :! :n
-- :doc saves a new image to db
INSERT INTO images
(name, type, entry_id, data)
VALUES (:name, :type, :entry_id, :data)

-- :name get-images :? :*
-- :doc gets all images
SELECT  * FROM images

-- :name get-image :? :1
-- :doc gets all images
SELECT  * FROM images
WHERE name = :name

-- :name delete-image! :! :n
-- :doc deletes image with given name
DELETE FROM images
WHERE name = :name