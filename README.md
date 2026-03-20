# COMP421 Group 76 - Project 3

This repository contains our work for COMP 421 Project 3 ("Writing your Application"), Winter 2026.

## Project Scope
The project uses the course DB2 instance and builds on our earlier project phases. The deliverable includes:

- a stored procedure
- a Java JDBC application with a menu loop
- indexing work
- one data visualization artifact
- optional creativity extension(s)

## Java + DB2 Setup
The DB2 JDBC driver `db2jcc4.jar` is included in `lib/`.

Cursor/VS Code resolves the driver through `.vscode/settings.json`:

`"java.project.referencedLibraries": ["lib/**/*.jar"]`

If you get errors like `com.ibm.db2.jcc.DB2Driver cannot be resolved`, verify:

- the file is `lib/db2jcc4.jar` (not a `.zip`)
- the referenced libraries setting above is present
- the Java workspace has been reloaded/cleaned

**Terminal compile/run:** `.vscode/settings.json` only applies to the IDE. From the command line you must pass the driver on the classpath (`-cp`) for both `javac` and `java`.

## Environment Variables for DB Login
Set credentials before running the JDBC program.

PowerShell (Windows):

`$env:SOCSUSER="yoursocsuserid"`

`$env:SOCSPASSWD="yoursocspasswd"`

Bash (Linux/macOS):

`export SOCSUSER=yoursocsuserid`

`export SOCSPASSWD=yoursocspasswd`

## Compile and Run
Compile and run the connection smoke test from the project root (include `lib/db2jcc4.jar` so `com.ibm.db2.jcc` resolves):

**Windows (PowerShell / cmd):**
Compile:
`javac -cp "lib/db2jcc4.jar" -d out src/draftline.java test/DBConnectionSmokeTest.java`

Run:
`java -cp "out;lib/db2jcc4.jar" DBConnectionSmokeTest`

**Linux / macOS (bash):**
Compile:
`javac -cp "lib/db2jcc4.jar" -d out src/draftline.java test/DBConnectionSmokeTest.java`

Run:
`java -cp "out:lib/db2jcc4.jar" DBConnectionSmokeTest`

## Makefile
From the project root (Git Bash / Linux / macOS; on Windows use Git Bash so `make` is available):

| Command | Action |
|--------|--------|
| `make` or `make compile` | Compile all `src/*.java` and `test/*.java` into `out/` |
| `make run` | Run `draftline` |
| `make test` | Run `DBConnectionSmokeTest` |
| `make clean` | Remove `out/` |
| `make help` | Print targets |

Set `SOCSUSER` and `SOCSPASSWD` before `make run` or `make test`. The Makefile picks `;` vs `:` for the Java classpath automatically on Windows vs Unix.