/* ---------------------------------PROCEDURES------------------------------------- */

DELIMITER $
CREATE PROCEDURE financial_info(IN branch_code INT(11), OUT income DECIMAL(10,2), OUT expenses DECIMAL(10,2), OUT profit DECIMAL(10,2))
BEGIN
    DECLARE br_id INT(11);

    IF NOT EXISTS(SELECT br_code FROM branch WHERE br_code = branch_code) THEN
        SET br_id = NULL;
    ELSE
        SET br_id = branch_code;
    END IF;

    IF br_id IS NULL THEN
        SET income = NULL;
        SET expenses = NULL;
        SET profit = NULL;
    ELSE
        SELECT SUM(res_total_cost) INTO income FROM reservation INNER JOIN trip ON reservation.res_tr_id = trip.tr_id WHERE trip.tr_br_code = br_id;
        SELECT SUM(wrk_salary) INTO expenses FROM worker WHERE wrk_br_code = br_id;
        IF expenses > 0 THEN
            SET profit = (income - expenses) / expenses;
        ELSE
            SET profit = NULL;
        END IF;
    END IF;
END $
DELIMITER ;



DELIMITER $
CREATE PROCEDURE vehicle_assignment(IN trip_code INT(11), IN vehicle_code INT(11), IN total_km INT(11))
    BEGIN
        DECLARE num_of_seats INT(11);
        DECLARE type_of_vehicle ENUM('CAR', 'VAN', 'MINIBUS', 'BUS');
        DECLARE vehicle_capacity TINYINT(4);
        DECLARE vehicle_status ENUM('AVAILABLE', 'RESERVED', 'MAINTENANCE');
        DECLARE driver_license ENUM('A', 'B', 'C', 'D');
        DECLARE trip_status ENUM('PLANNED', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED');
        DECLARE departure_time DATETIME;
        DECLARE return_time DATETIME;
        DECLARE overlap_count INT;

        SELECT type, status, capacity INTO type_of_vehicle, vehicle_status, vehicle_capacity FROM vehicle WHERE vehicle_id = vehicle_code;
        SELECT COUNT(*) INTO num_of_seats FROM reservation WHERE res_tr_id = trip_code AND res_status != 'CANCELLED';
        SELECT drv_license INTO driver_license FROM driver INNER JOIN trip ON trip.tr_drv_AT = driver.drv_AT WHERE trip.tr_id = trip_code;
        SELECT tr_departure, tr_return, tr_status INTO departure_time, return_time, trip_status FROM trip WHERE tr_id = trip_code;
        SELECT COUNT(*) INTO overlap_count FROM trip WHERE tr_vehicle_id = vehicle_code AND tr_id != trip_code AND tr_status != 'CANCELLED' AND (tr_departure <= return_time AND tr_return >= departure_time);

        IF vehicle_status != 'AVAILABLE' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle is not available!';
        ELSEIF overlap_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle is reserved by another trip!';
        ELSEIF trip_status != 'PLANNED' AND trip_status != 'CONFIRMED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot assign vehicle to this trip!';
        ELSEIF num_of_seats > vehicle_capacity THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Not enough seats in the vehicle!';
        ELSEIF vehicle_capacity > 9 && (driver_license = 'A' OR driver_license = 'B') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Driver is not allowed to drive this vehicle!';
        ELSE
            UPDATE trip SET tr_vehicle_id = vehicle_code WHERE tr_id = trip_code;
            UPDATE vehicle SET status = 'RESERVED' WHERE vehicle_id = vehicle_code;
            UPDATE vehicle SET total_miles = total_km WHERE vehicle_id = vehicle_code;
            SELECT 'Vehicle successfully assigned!';
        END IF;
     END $
DELIMITER ;


DELIMITER $
CREATE PROCEDURE find_availability(
    IN dst_id INT,
    IN date_in DATE,
    IN date_out DATE,
    IN num_rooms INT,
    OUT acc_id INT
)
BEGIN
    SET acc_id = NULL;

    SELECT a.acc_id INTO acc_id
    FROM accommodation a
    WHERE a.acc_destination_id = dst_id
      AND a.acc_status = 'active'
      AND (
              a.acc_rooms - IFNULL(
                      (SELECT SUM(CEIL(participants/2))
                       FROM (
                                SELECT COUNT(r.res_seatnum) as participants
                                FROM trip_accommodation ta
                                         JOIN reservation r ON ta.tr_id = r.res_tr_id
                                WHERE ta.acc_id = a.acc_id
                                  AND r.res_status != 'CANCELLED'
                                  AND NOT (ta.check_out_date <= date_in OR ta.check_in_date >= date_out)
                                GROUP BY ta.tr_id
                            ) as subquery_1
                      ), 0)
              ) >= num_rooms
    ORDER BY a.acc_room_price ASC, a.acc_stars DESC, a.acc_rating DESC
    LIMIT 1;

    SELECT
        a.acc_id,
        a.acc_name,
        a.acc_type,
        a.acc_address,
        a.acc_phone,
        a.acc_stars,
        a.acc_rating,
        a.acc_room_price,

        GROUP_CONCAT(am.am_name SEPARATOR ', ') AS amenities,

        (a.acc_rooms - IFNULL(
                (SELECT SUM(CEIL(participants/2))
                 FROM (
                          SELECT COUNT(r.res_seatnum) as participants
                          FROM trip_accommodation ta
                                   JOIN reservation r ON ta.tr_id = r.res_tr_id
                          WHERE ta.acc_id = a.acc_id
                            AND r.res_status != 'CANCELLED'
                            AND NOT (ta.check_out_date <= date_in OR ta.check_in_date >= date_out)
                          GROUP BY ta.tr_id
                      ) as subquery_2
                ), 0)
            ) AS available_rooms

    FROM accommodation a
             LEFT JOIN accommodation_amenity aa ON a.acc_id = aa.acc_id
             LEFT JOIN amenity am ON aa.am_id = am.am_id

    WHERE a.acc_destination_id = dst_id
      AND a.acc_status = 'active'
    GROUP BY a.acc_id

    HAVING available_rooms >= num_rooms

    ORDER BY a.acc_room_price ASC, a.acc_stars DESC, a.acc_rating DESC;

END$
DELIMITER ;



DELIMITER $
CREATE PROCEDURE make_booking(
    IN in_tr_id INT
)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE var_dst_id INT;
    DECLARE var_arrival DATETIME;
    DECLARE var_departure DATETIME;
    DECLARE var_seats INT;
    DECLARE var_acc_id INT;
    DECLARE var_rooms INT;
    DECLARE error_occured BOOLEAN DEFAULT FALSE;

    DECLARE cursor_destinations CURSOR FOR
        SELECT to_dst_id, to_arrival, to_departure
        FROM travel_to
        WHERE to_tr_id = in_tr_id
        ORDER BY to_sequence ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            SELECT 'Error: Transaction failed and rolled back.' AS Message;
        END;

    SELECT tr_maxseats INTO var_seats FROM trip WHERE tr_id = in_tr_id;
    SET var_rooms = CEIL(var_seats/2);

    START TRANSACTION;

    OPEN cursor_destinations;

    read_loop: LOOP
        FETCH cursor_destinations INTO var_dst_id, var_arrival, var_departure;

        IF done THEN
            LEAVE read_loop;
        END IF;

        CALL find_availability(var_dst_id, DATE(var_arrival), DATE(var_departure), var_rooms, var_acc_id);

        IF var_acc_id IS NOT NULL THEN
            INSERT INTO trip_accommodation (tr_id, acc_id, check_in_date, check_out_date)
            VALUES (in_tr_id, var_acc_id, DATE(var_arrival), DATE(var_departure));
        ELSE
            SET error_occured = TRUE;
            LEAVE read_loop;
        END IF;
    END LOOP;

    CLOSE cursor_destinations;

    IF error_occured THEN
        ROLLBACK;
        SELECT CONCAT('Reservation failed: No accommodation found for destination ID ', var_dst_id) AS Status;
    ELSE
        COMMIT;

        SELECT
            ta.tr_id,
            d.dst_name,
            a.acc_name,
            ta.check_in_date,
            ta.check_out_date,
            DATEDIFF(ta.check_out_date, ta.check_in_date) AS nights,
            (DATEDIFF(ta.check_out_date, ta.check_in_date) * a.acc_room_price * var_rooms) AS total_cost
        FROM trip_accommodation ta
                 JOIN accommodation a ON ta.acc_id = a.acc_id
                 JOIN travel_to tt ON ta.tr_id = tt.to_tr_id AND a.acc_destination_id = tt.to_dst_id
                 JOIN destination d ON tt.to_dst_id = d.dst_id
        WHERE ta.tr_id = in_tr_id;
    END IF;
END$
DELIMITER ;



DELIMITER $
CREATE PROCEDURE GenerateTripHistory()
BEGIN
    DECLARE i INT DEFAULT 0;

    WHILE i < 95000 DO
     INSERT INTO trip_history (trip_id, departure_date, return_date, count_destinations, count_participants, total_revenue)
       VALUES(
       FLOOR(1 + RAND() * 1000),
       DATE_ADD('2020-01-01', INTERVAL FLOOR(RAND() * 1800) DAY),
       DATE_ADD('2020-01-05', INTERVAL FLOOR(RAND() * 1800) DAY),
       FLOOR(1 + RAND() * 5),
       FLOOR(10 + RAND() * 40),
       FLOOR(1000 + RAND() * 50000)
    );
    SET i = i + 1;
   END WHILE;
END$
DELIMITER ;



DELIMITER $
CREATE PROCEDURE GetRevenueStats(IN start_d DATE, IN end_d DATE)
BEGIN
    SELECT SUM(total_revenue) as Range_Revenue
    FROM trip_history
    WHERE departure_date BETWEEN start_d AND end_d;
END$

DROP PROCEDURE IF EXISTS GetTripsByDestinations$
CREATE PROCEDURE GetTripsByDestinations(IN dest_count INT)
BEGIN
    SELECT count(*) as Trips_Found
    FROM trip_history
    WHERE count_destinations = dest_count;
END$
DELIMITER ;

/* ---------------------------------TRIGGERS------------------------------------- */

DELIMITER $
CREATE TRIGGER salary_check BEFORE UPDATE ON worker FOR EACH ROW
    BEGIN
        DECLARE income DECIMAL(10,2);
        DECLARE expenses DECIMAL(10,2);
        DECLARE profit DECIMAL(10,2);

        IF NEW.wrk_salary > OLD.wrk_salary THEN
            CALL financial_info(OLD.wrk_br_code, income, expenses, profit);
            IF profit < 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Branch profit is negative! Cannot increase salary!';
            ELSEIF profit IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Branch either does not exist or has no reservations!';
            ELSE
                IF (NEW.wrk_salary > OLD.wrk_salary + OLD.wrk_salary * 0.02) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Salary increase is too high! Cannot exceed 2% of the current salary!';
                END IF;
            END IF;
        END IF;
    END $
DELIMITER ;



DELIMITER $
CREATE TRIGGER update_vehicle AFTER UPDATE ON trip FOR EACH ROW
    BEGIN
        IF NEW.tr_status = 'COMPLETED' && OLD.tr_status != 'COMPLETED' THEN
            UPDATE vehicle SET status = 'AVAILABLE', total_miles = total_miles + NEW.miles_driven WHERE vehicle_id = NEW.tr_vehicle_id;
        END IF;
    END $
DELIMITER ;



DELIMITER $
CREATE TRIGGER calculate_accommodation_cost BEFORE INSERT ON trip_accommodation
FOR EACH ROW
BEGIN
    DECLARE v_price DECIMAL(10,2);
    DECLARE v_maxseats INT;
    DECLARE v_rooms INT;

    SELECT acc_room_price INTO v_price
    FROM accommodation
    WHERE acc_id = NEW.acc_id;

    SELECT tr_maxseats INTO v_maxseats
    FROM trip
    WHERE tr_id = NEW.tr_id;

    SET v_rooms = CEIL(v_maxseats / 2);
    SET NEW.tac_nights = DATEDIFF(NEW.check_out_date, NEW.check_in_date);
    SET NEW.tac_cost = NEW.tac_nights * v_price * v_rooms;
END$

DELIMITER ;



DELIMITER $
CREATE TRIGGER log_trip_insert AFTER INSERT ON trip
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'trip', CONCAT('New trip created with ID: ', NEW.tr_id));
END$

CREATE TRIGGER log_trip_update AFTER UPDATE ON trip
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'trip', CONCAT('Trip ID ', OLD.tr_id, ' status changed from ', OLD.tr_status, ' to ', NEW.tr_status));
END$

CREATE TRIGGER log_trip_delete AFTER DELETE ON trip
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'trip', CONCAT('Trip deleted with ID: ', OLD.tr_id));
END$

CREATE TRIGGER log_reservation_insert AFTER INSERT ON reservation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'reservation', CONCAT('New reservation for Trip ID: ', NEW.res_tr_id, ', Seat: ', NEW.res_seatnum));
END$

CREATE TRIGGER log_reservation_update AFTER UPDATE ON reservation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'reservation', CONCAT('Reservation changed for Trip ID: ', OLD.res_tr_id, '. Status: ', OLD.res_status, ' -> ', NEW.res_status));
END$

CREATE TRIGGER log_reservation_delete AFTER DELETE ON reservation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'reservation', CONCAT('Reservation deleted for Trip ID: ', OLD.res_tr_id, ', Seat: ', OLD.res_seatnum));
END$

CREATE TRIGGER log_vehicle_insert AFTER INSERT ON vehicle
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'vehicle', CONCAT('New vehicle added: ', NEW.brand, ' ', NEW.model, ' (', NEW.plate_number, ')'));
END$

CREATE TRIGGER log_vehicle_update AFTER UPDATE ON vehicle
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'vehicle', CONCAT('Vehicle ', OLD.plate_number, ' updated. Status: ', OLD.status, ' -> ', NEW.status));
END$

CREATE TRIGGER log_vehicle_delete AFTER DELETE ON vehicle
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'vehicle', CONCAT('Vehicle deleted: ', OLD.plate_number));
END$

CREATE TRIGGER log_customer_insert AFTER INSERT ON customer
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'customer', CONCAT('New customer created: ', NEW.cust_name, ' ', NEW.cust_lname, ' (ID: ', NEW.cust_id, ')'));
END$

CREATE TRIGGER log_customer_update AFTER UPDATE ON customer
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'customer', CONCAT('Customer ID ', OLD.cust_id, ' details updated.'));
END$

CREATE TRIGGER log_customer_delete AFTER DELETE ON customer
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'customer', CONCAT('Customer deleted: ', OLD.cust_name, ' ', OLD.cust_lname, ' (ID: ', OLD.cust_id, ')'));
END$

CREATE TRIGGER log_destination_insert AFTER INSERT ON destination
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'destination', CONCAT('New destination added: ', NEW.dst_name, ' (ID: ', NEW.dst_id, ')'));
END$

CREATE TRIGGER log_destination_update AFTER UPDATE ON destination
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'destination', CONCAT('Destination ID ', OLD.dst_id, ' updated.'));
END$

CREATE TRIGGER log_destination_delete AFTER DELETE ON destination
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'destination', CONCAT('Destination deleted: ', OLD.dst_name, ' (ID: ', OLD.dst_id, ')'));
END$

CREATE TRIGGER log_accommodation_insert AFTER INSERT ON accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'accommodation', CONCAT('New accommodation added: ', NEW.acc_name, ' (ID: ', NEW.acc_id, ')'));
END$

CREATE TRIGGER log_accommodation_update AFTER UPDATE ON accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'accommodation', CONCAT('Accommodation ID ', OLD.acc_id, ' updated. Status: ', OLD.acc_status, ' -> ', NEW.acc_status));
END$

CREATE TRIGGER log_accommodation_delete AFTER DELETE ON accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'accommodation', CONCAT('Accommodation deleted: ', OLD.acc_name, ' (ID: ', OLD.acc_id, ')'));
END$

CREATE TRIGGER log_trip_acc_insert AFTER INSERT ON trip_accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'INSERT', 'trip_accommodation', CONCAT('Trip ID ', NEW.tr_id, ' booked at Accommodation ID ', NEW.acc_id));
END$

CREATE TRIGGER log_trip_acc_update AFTER UPDATE ON trip_accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'UPDATE', 'trip_accommodation', CONCAT('Booking updated for Trip ID ', OLD.tr_id, ' at Acc ID ', OLD.acc_id));
END$

CREATE TRIGGER log_trip_acc_delete AFTER DELETE ON trip_accommodation
FOR EACH ROW
BEGIN
    INSERT INTO system_log (log_user, log_action_type, log_table_name, log_description)
    VALUES (USER(), 'DELETE', 'trip_accommodation', CONCAT('Booking removed for Trip ID ', OLD.tr_id, ' at Acc ID ', OLD.acc_id));
END$

DELIMITER ;
