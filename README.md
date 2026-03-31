# COMP421 Group 76 - Project 3

## How to Run

### 1. Set credentials

PowerShell (Windows):
```
$env:SOCSUSER="yoursocsuserid"
$env:SOCSPASSWD="yoursocspasswd"
```

Bash (Linux/macOS):
```
export SOCSUSER=yoursocsuserid
export SOCSPASSWD=yoursocspasswd
```

### 2. Compile and run

```
make run
```

> **Note:** On Windows, use **PowerShell**, not Git Bash. Git Bash misresolves the Java classpath separator and will throw `ClassNotFoundException`.

---

## Application Menu

| # | Menu option | What it does |
|---|-------------|--------------|
| **1** | Look up player by last name | Search by family name, disambiguate if needed, then show profile, active/recent contracts, career goal count, and distinct games played. |
| **2** | Create a game and record goals | Pick a league, then home and away teams; set date, stage, venue, and referees; enroll all eligible players into PlayedIn via stored procedure; loop to record goals (scorer, minute, goal number, optional link); award points on finish. |
| **3** | Active roster — expiring contracts first | Pick a league, then a team. Show everyone on the roster today (active contract), sorted by earliest expiry. For each player: name, position, contract end date, career goals. |
| **4** | Register new person (player / coach / referee) | Insert into PERSON, then into PLAYER, COACH, or REFEREE with the same pid and role-specific columns. |
| **5** | Sign a player (new contract) | Find a player, choose team/league, jersey, and contract length; expire any overlapping active contract; insert the new row. |
| **6** | Quit | Exit; connection closes. |

---

## Common last names (useful for testing player lookup)

| Last name | Player rows |
|-----------|-------------|
| Roberts   | 4 |
| Johnson   | 3 |
| Moore     | 3 |
| Smith     | 3 |

---

## Other Make targets

| Command | Action |
|---------|--------|
| `make` or `make compile` | Compile all `src/*.java` and `test/*.java` into `out/` |
| `make run` | Compile and run `draftline` |
| `make test` | Run `DBConnectionSmokeTest` |
| `make clean` | Remove `out/` |
| `make help` | Print targets |

---

## Project overview

This project uses the course DB2 instance and builds on earlier project phases. The deliverable includes a stored procedure, a Java JDBC application with a menu loop, indexing work, one data visualization artifact, and optional creativity extensions.

The DB2 JDBC driver `db2jcc4.jar` is included in `lib/`. Cursor/VS Code resolves it through `.vscode/settings.json` (`"java.project.referencedLibraries": ["lib/**/*.jar"]`). From the terminal the Makefile handles the classpath automatically.
