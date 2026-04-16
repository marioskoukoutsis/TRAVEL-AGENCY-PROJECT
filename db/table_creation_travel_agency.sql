CREATE DATABASE travel_agency_2025;
USE travel_agency_2025;

CREATE TABLE customer(
    cust_id INT(11) NOT NULL AUTO_INCREMENT,
    cust_name VARCHAR(30) NOT NULL,
    cust_lname VARCHAR(30) NOT NULL,
    cust_email VARCHAR(100) NOT NULL,
    cust_phone VARCHAR(15) NOT NULL,
    cust_address TEXT NOT NULL,
    cust_birth_date DATE NOT NULL,
    PRIMARY KEY (cust_id)
);

CREATE TABLE language_ref(
    lang_code VARCHAR(5) NOT NULL,
    lang_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (lang_code)
);

CREATE TABLE admin(
    adm_AT CHAR(10),
    adm_type ENUM('LOGISTICS', 'ADMINISTRATIVE', 'ACCOUNTING'),
    adm_diploma VARCHAR(200),
    PRIMARY KEY (adm_AT)
);

CREATE TABLE branch(
    br_code INT(11) NOT NULL AUTO_INCREMENT,
    br_street VARCHAR(50) NOT NULL,
    br_num INT(4) NOT NULL,
    br_city VARCHAR(30) NOT NULL,
    br_manager_AT CHAR(10) NOT NULL,
    PRIMARY KEY (br_code)
);

CREATE TABLE worker(
    wrk_AT CHAR(10) NOT NULL,
    wrk_name VARCHAR(30) NOT NULL,
    wrk_lname VARCHAR(30) NOT NULL,
    wrk_email VARCHAR(100) NOT NULL,
    wrk_salary DECIMAL(10,2) NOT NULL,
    wrk_br_code INT(11),
    PRIMARY KEY (wrk_AT)
);

CREATE TABLE driver(
    drv_AT CHAR(10),
    drv_license ENUM('A', 'B', 'C', 'D'),
    drv_route ENUM('LOCAL', 'ABROAD'),
    drv_experience TINYINT(4) NOT NULL,
    PRIMARY KEY (drv_AT),
    CONSTRAINT WRKDRV FOREIGN KEY (drv_AT) REFERENCES worker(wrk_AT)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE guide(
    gui_AT CHAR(10),
    gui_cv TEXT,
    PRIMARY KEY (gui_AT),
    CONSTRAINT GUIWRK FOREIGN KEY (gui_AT) REFERENCES worker(wrk_AT)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE trip(
    tr_id INT(11) NOT NULL AUTO_INCREMENT,
    tr_departure DATETIME NOT NULL,
    tr_return DATETIME NOT NULL,
    tr_maxseats TINYINT(4) NOT NULL,
    tr_cost_adult DECIMAL(10,2) NOT NULL,
    tr_cost_child DECIMAL(10,2) NOT NULL,
    tr_status ENUM('PLANNED', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED'),
    tr_min_participants TINYINT(4) NOT NULL,
    tr_br_code INT(11) NOT NULL,
    tr_gui_AT CHAR(10) NOT NULL,
    tr_drv_AT CHAR(10) NOT NULL,
    PRIMARY KEY (tr_id),
    CONSTRAINT TRBRCODE FOREIGN KEY (tr_br_code) REFERENCES branch(br_code),
    CONSTRAINT TRGUI FOREIGN KEY (tr_gui_AT) REFERENCES guide(gui_AT),
    CONSTRAINT TRDRV FOREIGN KEY (tr_drv_AT) REFERENCES driver(drv_AT)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE languages(
    lng_gui_AT CHAR(10),
    lng_language_code VARCHAR(5),
    PRIMARY KEY (lng_gui_AT, lng_language_code),
    CONSTRAINT LNGGUI FOREIGN KEY (lng_gui_AT) REFERENCES guide(gui_AT),
    CONSTRAINT LNGCODE FOREIGN KEY (lng_language_code) REFERENCES language_ref(lang_code)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE manages(
    mng_adm_AT CHAR(10),
    mng_br_code INT(11),
    PRIMARY KEY (mng_adm_AT, mng_br_code),
    CONSTRAINT MNGRADM FOREIGN KEY (mng_adm_AT) REFERENCES admin(adm_AT),
    CONSTRAINT MNGRBR FOREIGN KEY (mng_br_code) REFERENCES branch(br_code)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE phones(
    ph_br_code INT(11),
    ph_number VARCHAR(15) NOT NULL,
    PRIMARY KEY (ph_br_code, ph_number),
    CONSTRAINT PHONEBR FOREIGN KEY (ph_br_code) REFERENCES branch(br_code)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE destination(
    dst_id INT(11) NOT NULL AUTO_INCREMENT,
    dst_name VARCHAR(100) NOT NULL,
    dst_descr TEXT,
    dst_rtype ENUM('LOCAL', 'ABROAD'),
    dst_language_code VARCHAR(5),
    dst_location INT(11),
    PRIMARY KEY (dst_id),
    CONSTRAINT DSTLANG FOREIGN KEY (dst_language_code) REFERENCES language_ref(lang_code),
    CONSTRAINT LOCATIONID FOREIGN KEY (dst_location) REFERENCES destination(dst_id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE travel_to(
    to_tr_id INT(11),
    to_dst_id INT(11),
    to_arrival DATETIME NOT NULL,
    to_departure DATETIME NOT NULL,
    to_sequence TINYINT(4) NOT NULL,
    PRIMARY KEY (to_tr_id, to_dst_id),
    CONSTRAINT TRIPTRVLTO FOREIGN KEY (to_tr_id) REFERENCES trip(tr_id),
    CONSTRAINT TRVLTODEST FOREIGN KEY (to_dst_id) REFERENCES destination(dst_id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE event(
    ev_tr_id INT(11),
    ev_start DATETIME NOT NULL,
    ev_end DATETIME NOT NULL,
    ev_descr TEXT,
    PRIMARY KEY (ev_tr_id, ev_start),
    CONSTRAINT TRPEVENT FOREIGN KEY (ev_tr_id) REFERENCES trip(tr_id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE reservation(
    res_tr_id INT(11),
    res_seatnum TINYINT(4) NOT NULL,
    res_cust_id INT(11),
    res_status ENUM('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED'),
    res_total_cost DECIMAL(10,2),
    PRIMARY KEY (res_tr_id, res_seatnum),
    CONSTRAINT RESTRIP FOREIGN KEY (res_tr_id) REFERENCES trip(tr_id),
    CONSTRAINT CUSTRES FOREIGN KEY (res_cust_id) REFERENCES customer(cust_id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE vehicle(
    vehicle_id INT(11) NOT NULL AUTO_INCREMENT,
    brand VARCHAR(30) NOT NULL,
    model VARCHAR(30) NOT NULL,
    plate_number VARCHAR(10) NOT NULL UNIQUE,
    capacity TINYINT(4) NOT NULL,
    type ENUM('CAR', 'VAN', 'MINIBUS', 'BUS') NOT NULL,
    status ENUM('AVAILABLE', 'RESERVED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    total_miles INT(11) NOT NULL,
    PRIMARY KEY (vehicle_id)
);

CREATE TABLE accommodation (
    acc_id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    acc_name VARCHAR(30) NOT NULL,

    acc_type ENUM('Hotel', 'Hostel', 'Resort', 'Apartment', 'Room') NOT NULL,

    acc_stars INT CHECK (acc_stars BETWEEN 1 AND 5),
    acc_rating DECIMAL(3,2) DEFAULT 0.00 CHECK (acc_rating >= 0.00 AND acc_rating <= 5.00),

    acc_status VARCHAR(30) NOT NULL DEFAULT 'Active' CHECK (acc_status IN ('Active', 'Renovation', 'Ended')),

    acc_address VARCHAR(30) NOT NULL,
    acc_zipcode VARCHAR(30),
    acc_city VARCHAR(30),
    acc_phone VARCHAR(30),
    acc_email VARCHAR(30),

    acc_rooms INT NOT NULL CHECK (acc_rooms>0),
    acc_room_price DECIMAL(10,2) NOT NULL,

    acc_destination_id INT NOT NULL,
    FOREIGN KEY (acc_destination_id) REFERENCES destination(dst_id)
    ON DELETE CASCADE
);

CREATE TABLE amenity (
    am_id INT NOT NULL AUTO_INCREMENT,
    am_name VARCHAR(30) NOT NULL,

    PRIMARY KEY (am_id)	
);

CREATE TABLE accommodation_amenity (
    acc_id INT(11) NOT NULL,
    am_id INT(11) NOT NULL,

    PRIMARY KEY (acc_id, am_id),
    CONSTRAINT fk_acc_amenities_acc FOREIGN KEY (acc_id) REFERENCES accommodation(acc_id) ON DELETE CASCADE,
    CONSTRAINT fk_acc_amenities_am FOREIGN KEY (am_id) REFERENCES amenity(am_id) ON DELETE RESTRICT
);

CREATE TABLE trip_accommodation (
    id INT(10) NOT NULL AUTO_INCREMENT,
    tr_id INT(10) NOT NULL,
    acc_id INT(10) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trip_acc_trip FOREIGN KEY (tr_id) REFERENCES trip(tr_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_trip_acc_acc FOREIGN KEY (acc_id) REFERENCES accommodation(acc_id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE trip_history (
    hist_id INT AUTO_INCREMENT PRIMARY KEY,
    trip_id INT NOT NULL,
    departure_date DATETIME,
    return_date DATETIME,
    count_destinations INT,
    count_participants INT,
    total_revenue DECIMAL(10, 2)
);

CREATE TABLE system_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    log_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    log_user VARCHAR(50) NOT NULL,
    log_action_type ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    log_table_name VARCHAR(50) NOT NULL,
    log_description TEXT
);

CREATE TABLE dba_admin (
    dba_username VARCHAR(50) NOT NULL,
    dba_start_date DATE NOT NULL,
    dba_end_date DATE NULL,
    PRIMARY KEY (dba_username)
);

ALTER TABLE worker ADD
CONSTRAINT WRKBRCODE FOREIGN KEY (wrk_br_code) REFERENCES branch(br_code)
ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE branch ADD
CONSTRAINT BRMNGADM FOREIGN KEY (br_manager_AT) REFERENCES admin(adm_AT)
ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE admin ADD
CONSTRAINT ADMWRK FOREIGN KEY (adm_AT) REFERENCES worker(wrk_AT)
ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE trip ADD COLUMN tr_vehicle_id INT(11);

ALTER TABLE trip ADD CONSTRAINT TRVEHICLE FOREIGN KEY (tr_vehicle_id) REFERENCES vehicle(vehicle_id)
ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE trip ADD COLUMN miles_driven INT DEFAULT 0;

ALTER TABLE trip_accommodation
ADD COLUMN tac_nights INT DEFAULT 0,
ADD COLUMN tac_cost DECIMAL(10,2) DEFAULT 0.00;
CREATE INDEX idx_hist_date ON trip_history(departure_date);
CREATE INDEX idx_hist_dest ON trip_history(count_destinations);