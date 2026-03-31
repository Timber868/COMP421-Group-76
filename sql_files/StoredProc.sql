-- Stored procedure for Project 3:
-- Given a game id, add every player who has an active contract for either the
-- home team or away team on that game's date into PLAYEDIN.

CREATE OR REPLACE PROCEDURE CS421G76.REGISTER_ELIGIBLE_PLAYERS_FOR_GAME
(
    IN p_gid CHAR(36),
    OUT p_inserted INT,
    OUT p_already_present INT
)
LANGUAGE SQL
BEGIN
    DECLARE v_done SMALLINT DEFAULT 0;
    DECLARE v_game_date DATE;
    DECLARE v_home_team VARCHAR(255);
    DECLARE v_home_league VARCHAR(255);
    DECLARE v_away_team VARCHAR(255);
    DECLARE v_away_league VARCHAR(255);
    DECLARE v_pid CHAR(36);

    DECLARE cur_players CURSOR FOR
        SELECT DISTINCT c.pid
        FROM CS421G76.Contract c
        WHERE v_game_date BETWEEN c.valid_from AND c.valid_until
          AND (
                (c.team_name = v_home_team AND c.league_name = v_home_league)
             OR (c.team_name = v_away_team AND c.league_name = v_away_league)
          )
        ORDER BY c.pid;

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_done = 1;

    SET p_inserted = 0;
    SET p_already_present = 0;

    SELECT g.date
    INTO v_game_date
    FROM CS421G76.Game g
    WHERE g.gid = p_gid;

    IF v_game_date IS NULL THEN
        SIGNAL SQLSTATE '75001'
            SET MESSAGE_TEXT = 'Game id not found in CS421G76.GAME.';
    END IF;

    SELECT h.team_name, h.league_name
    INTO v_home_team, v_home_league
    FROM CS421G76.GameHomeTeam h
    WHERE h.gid = p_gid
    FETCH FIRST 1 ROW ONLY;

    IF v_home_team IS NULL OR v_home_league IS NULL THEN
        SIGNAL SQLSTATE '75002'
            SET MESSAGE_TEXT = 'Home team row missing for this game.';
    END IF;

    SELECT a.team_name, a.league_name
    INTO v_away_team, v_away_league
    FROM CS421G76.GameAwayTeam a
    WHERE a.gid = p_gid
    FETCH FIRST 1 ROW ONLY;

    IF v_away_team IS NULL OR v_away_league IS NULL THEN
        SIGNAL SQLSTATE '75003'
            SET MESSAGE_TEXT = 'Away team row missing for this game.';
    END IF;

    OPEN cur_players;

    read_loop:
    LOOP
        FETCH cur_players INTO v_pid;

        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM CS421G76.PlayedIn pi
            WHERE pi.pid = v_pid
              AND pi.gid = p_gid
        ) THEN
            SET p_already_present = p_already_present + 1;
        ELSE
            INSERT INTO CS421G76.PlayedIn(pid, gid)
            VALUES (v_pid, p_gid);

            SET p_inserted = p_inserted + 1;
        END IF;
    END LOOP;

    CLOSE cur_players;
END@
