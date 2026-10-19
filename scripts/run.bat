@echo off
cd /d "%~dp0\.."
if exist releases\placesure.jar (
  java -jar releases\placesure.jar %*
) else (
  java -jar target\placesure.jar %*
)
