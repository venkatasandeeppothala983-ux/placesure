-- The Blueprint fixes MYSQL_DATABASE=placesure and MYSQL_USER=placesure_app.
-- The official entrypoint provisions that user before running these initialization files.
-- Schema creation and demo imports run as root; the Java app only gets CRUD permissions.
REVOKE ALL PRIVILEGES ON placesure.* FROM 'placesure_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON placesure.* TO 'placesure_app'@'%';
