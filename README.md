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
