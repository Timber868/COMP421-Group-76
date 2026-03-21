# COMP421 Group 76 - Project 3

This repository contains our work for COMP 421 Project 3 ("Writing your Application"), Winter 2026.

## Project Scope
The project uses the course DB2 instance and builds on our earlier project phases. The deliverable includes:

- a stored procedure
- a Java JDBC application with a menu loop
- indexing work
- one data visualization artifact
- optional creativity extension(s)

## Common last names (our data)

`PERSON.NAME` is stored as a single string (`First Last`). To see which surnames appear most often among **players**, group on the part after the first space (same simplifying rule as in ad hoc SQL; names with multiple spaces in the first name are grouped differently).

Recent ranking from our database (good values to try in **player lookup**, menu option 1 in `draftline`):

| Last name | Player rows |
|-----------|-------------|
| Roberts   | 4 |
| Johnson   | 3 |
| Moore     | 3 |
| Smith     | 3 |

Other surnames also rank near the top; re-run your `GROUP BY` query on `PERSON` joined to `PLAYER` to refresh counts after reloads.

Example query:

```sql
SELECT
  UPPER(TRIM(SUBSTR(RTRIM(per.name), LOCATE(' ', RTRIM(per.name)) + 1))) AS last_name,
  COUNT(*) AS how_many
FROM CS421G76.PERSON per
JOIN CS421G76.PLAYER pl ON per.pid = pl.pid
GROUP BY UPPER(TRIM(SUBSTR(RTRIM(per.name), LOCATE(' ', RTRIM(per.name)) + 1)))
ORDER BY how_many DESC, last_name
FETCH FIRST 25 ROWS ONLY;
```

## How player lookup works

The JDBC app’s **“Look up player by last name”** option is implemented in `src/PlayerLookupTask.java` and invoked from `src/draftline.java` (main menu option **1**).

1. **Input** — The user enters a last name (trimmed). Empty input is rejected with a short message.
2. **Match** — The program selects rows from `CS421G76.PERSON` joined to `CS421G76.PLAYER` where:
   - `NAME` ends with a space plus the given last name (case-insensitive pattern `'% LAST'`), **or**
   - `NAME` equals the whole string (single-token names).
   Only people who have a `PLAYER` row are returned.
3. **No matches** — Prints that no player was found for that last name.
4. **One match** — Prints the single summary line (full name, pid, nationality, birth date) and continues.
5. **Several matches** — Prints a numbered list of the same summary lines; the user picks `1`–`n`. Invalid or out-of-range input ends the task without showing a profile.
6. **Profile** — For the chosen player id (`PID` is a UUID string in our schema, not an integer), the app prints:
   - **Identity / player attributes:** name, nationality, birth date, position, dominant hand, debut date.
   - **Career stats:** total goals (`GOAL`), distinct games appeared in (`PLAYEDIN`).
   - **Contracts:** rows where `CURRENT DATE` is between `VALID_FROM` and `VALID_UNTIL`; if none, up to three most recent contracts by `VALID_UNTIL`, with a note that nothing is active “as of today.”

SQL errors are caught, printed with `SQLSTATE` / `SQLCODE`, and the menu loop continues; the shared connection is still closed when the user quits the app.

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