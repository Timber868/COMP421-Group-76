# COMP421 Group 76 — build and run from project root.
# Requires: JDK on PATH. On Windows, use Git Bash (or WSL) so `make`, `mkdir -p`, and `rm` work.
# Set SOCSUSER and SOCSPASSWD before `make run` or `make test` (see README).

JAR := lib/db2jcc4.jar
OUT := out
JAVAC := javac
JAVA := java

# Windows JVM uses ';' between classpath entries; Linux/macOS use ':'.
ifeq ($(OS),Windows_NT)
  CPSEP := ;
else
  CPSEP := :
endif

RUN_CP := $(OUT)$(CPSEP)$(JAR)
SOURCES := $(wildcard src/*.java src/**/*.java test/*.java)

.PHONY: all compile run test clean help

# Default: compile all sources under src/ and test/.
all: compile

help:
	@echo "Targets:"
	@echo "  make compile  - compile src/*.java and test/*.java into $(OUT)/"
	@echo "  make run      - run the main app (draftline)"
	@echo "  make test     - run DBConnectionSmokeTest"
	@echo "  make clean    - remove $(OUT)/"
	@echo "Set SOCSUSER and SOCSPASSWD before run/test."

$(OUT):
	mkdir -p $(OUT)

compile: $(OUT)
	$(JAVAC) -cp "$(JAR)" -d $(OUT) $(SOURCES)

run: compile
	$(JAVA) -cp "$(RUN_CP)" draftline

test: compile
	$(JAVA) -cp "$(RUN_CP)" DBConnectionSmokeTest

clean:
	rm -rf $(OUT)
