-- name: find-titles-by-address
SELECT url.title FROM url WHERE url.address = ?

-- name: find-last-msgid
SELECT msg.id FROM msg ORDER BY id DESC

-- name: find-urlid-by-address
SELECT url.id FROM url WHERE url.address = ?

-- name: find-urls-by-address
SELECT * FROM url
WHERE url.address = ?

-- name: insert-url!
INSERT INTO url VALUES (NULL, ?, ?)

-- name: insert-url-msg!
INSERT INTO url_msg VALUES (NULL, ?, ?)

-- name: insert-msg!
INSERT INTO msg VALUES (NULL, ?, ?, ?, CURRENT_DATE, CURRENT_TIME)

-- name: find-urls-by-target
SELECT url.title, url.address
FROM url
JOIN url_msg ON url.id = url_msg.urlid
JOIN msg ON msg.id = url_msg.msgid
WHERE msg.target = ? AND msg.date = ?
ORDER BY url.id DESC
