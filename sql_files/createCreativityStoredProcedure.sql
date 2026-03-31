CONNECT TO COMP421@

CREATE PROCEDURE CREATE_CONTRACT_FROM_TRADE(IN player_id char(36),
                                            IN new_cid char(36),
                                            IN new_tid char(36),
                                            IN in_fee DOUBLE,
                                            IN in_transfer_type varchar(255),
                                            IN in_from_team_name varchar(255),
                                            IN in_from_league_name varchar(255),
                                            IN in_to_team_name varchar(255),
                                            IN in_to_league_name varchar(255))
BEGIN
    DECLARE jersey_num_to_keep INT DEFAULT NULL;
    DECLARE until_date_to_keep DATE;

    DECLARE curr_cid CHAR(36);
    DECLARE curr_team_name VARCHAR(255);
    DECLARE curr_league_name VARCHAR(255);
    DECLARE curr_valid_until DATE;
    DECLARE curr_jersey_num INT;

    DECLARE latest_date DATE;

    DECLARE at_end INT DEFAULT 0;
    DECLARE not_found CONDITION FOR SQLSTATE '02000';

    DECLARE C1 CURSOR FOR
        SELECT cid, team_name, league_name, valid_until, jersey_number FROM Contract WHERE pid = player_id AND valid_until > CURRENT_DATE;
    DECLARE CONTINUE HANDLER FOR not_found SET at_end = 1;

    SET latest_date = CURRENT_DATE - 1 DAY;
    OPEN C1;
    FETCH C1 INTO curr_cid, curr_team_name, curr_league_name, curr_valid_until, curr_jersey_num;
        WHILE at_end = 0 DO
            IF (curr_valid_until > latest_date AND in_from_team_name = curr_team_name AND in_from_league_name = curr_league_name)
                THEN
                    SET until_date_to_keep = curr_valid_until;
                    SET latest_date = curr_valid_until;
                    SET jersey_num_to_keep = curr_jersey_num;
            end if;
            UPDATE Contract SET valid_until = (CURRENT_DATE - 1 DAY) WHERE cid = curr_cid;
            FETCH C1 INTO curr_cid, curr_team_name, curr_league_name, curr_valid_until, curr_jersey_num;
        end while;
    CLOSE C1;
    IF (jersey_num_to_keep IS NOT NULL)
        THEN
            INSERT INTO Contract VALUES (new_cid, player_id, in_to_team_name, in_to_league_name, CURRENT_DATE , until_date_to_keep, jersey_num_to_keep);
            INSERT INTO TRANSFER VALUES (new_tid, CURRENT_DATE, in_transfer_type, in_fee, in_from_team_name, in_from_league_name, in_to_team_name, in_to_league_name);
            INSERT INTO PLAYERSTRANSFERRED VALUES (player_id, new_tid);
    end if;
end;
@