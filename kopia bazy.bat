@echo off

SET db_name=logi
SET db_host=10.25.4.166
SET db_username=db
SET db_role=db_admin

SET pg_dump=%ProgramFiles(x86)%\pgAdmin III\1.20\pg_dump.exe
SET file=%db_name%_%date%_%random%.backup

echo.Rozpoczecie tworzenia kopii zapasowej bazy %db_host%/%db_name% do pliku %file%
echo.
pause
"%pg_dump%" --host %db_host% --port 5432^
 --username "%db_username%" --role "%db_role%" --no-password  --format custom --compress 9^
 --blobs --encoding UTF8 --verbose --file "%file%"  "%db_name%"
 
 rem --schema "public"
echo. 
echo.================= KONIEC =================
pause