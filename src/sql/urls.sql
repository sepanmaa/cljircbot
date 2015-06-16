SELECT url.title, url.address, msg.time
FROM url
JOIN url_msg ON url.id = url_msg.urlid
JOIN msg ON msg.id = url_msg.msgid
WHERE msg.target = ?
ORDER BY url.id DESC
