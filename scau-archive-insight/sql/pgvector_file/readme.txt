https://github.com/andreiramani/pgvector_pgsql_windows
------------------------------------------------------

- ONLY for PostgreSQL v18 - Windows x64
- Stop PostgreSQL using services.msc
- Extract the zip file to your Postgres installed folder
- Start PostgreSQL service
- Run query: 
CREATE EXTENSION vector
- Run this query to check if the extension is enable (t):
SELECT extname,extrelocatable,extversion FROM pg_extension where extname='vector'


=============================
UPGRADE FROM PREVIOUS VERSION
=============================
- Stop PostgreSQL using services.msc
- Extract the zip file to your Postgres installed folder, on overwrite dialog option, choose "Yes to all"
- Start PostgreSQL service
- Run query: 
ALTER EXTENSION vector UPDATE
- Check the version in the current database with:
SELECT extversion FROM pg_extension WHERE extname = 'vector'

