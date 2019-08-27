
-- USERS

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, username, first_name, last_name, pass)
VALUES (:id, :username, :first_name, :last_name, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET username = :username, first_name = :first_name, last_name = :last_name, pass = :pass
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-users :? :*
-- :doc retrieves a user record given the id
SELECT * FROM users

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- BLOG-ENTRIES

-- :name create-entry! :! :n
-- :doc creates a new blog entry
INSERT INTO blog_entries
(id, created, last_modified, author_id, header, summary, content)
VALUES (:id, :created, :last_modified, :author_id, :header, :summary, :content)

-- :name update-entry! :! :n
-- :doc updates an existing blog entry
UPDATE blog_entries
SET header = :header, summary = :summary, content = :content
WHERE id = :id

-- :name get-entries :? :*
-- :doc gets all blog entries
SELECT * FROM blog_entries

-- :name get-entry :? :1
-- :doc gets single entry
SELECT * FROM blog_entries
WHERE id = :id

-- :name delete-entry! :! :n
-- :doc deletes blog entry with given id
DELETE FROM blog_entries
WHERE id = :id
