# Private MySQL 8.4 service. /var/lib/mysql MUST be a persistent disk.
FROM mysql:8.4
COPY database/database.sql /docker-entrypoint-initdb.d/10-placesure.sql
COPY deploy/mysql/20-app-permissions.sql /docker-entrypoint-initdb.d/20-app-permissions.sql
CMD ["mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci", "--max-connections=50", "--innodb-buffer-pool-size=256M"]
