DROP DATABASE IF EXISTS sportscars;
CREATE DATABASE sportscars;
USE sportscars;

-- Reference Tables
CREATE TABLE make (
  make_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE engine (
  engine_id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  cylinders TINYINT NOT NULL,
  displacement_l DECIMAL(3,1) NOT NULL,
  aspiration ENUM('NA','Turbo','Supercharged') NOT NULL,
  horsepower INT NOT NULL,
  torque_nm INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE model (
  model_id INT AUTO_INCREMENT PRIMARY KEY,
  make_id INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  start_year SMALLINT,
  end_year SMALLINT,
  UNIQUE(make_id,name),
  INDEX idx_model_make (make_id),
  CONSTRAINT fk_model_make FOREIGN KEY (make_id) REFERENCES make(make_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `trim` (
  trim_id INT AUTO_INCREMENT PRIMARY KEY,
  model_id INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  msrp DECIMAL(12,2) NOT NULL,
  engine_id INT NOT NULL,
  transmission ENUM('Manual','DCT','Automatic') NOT NULL,
  drivetrain ENUM('RWD','AWD') NOT NULL,
  UNIQUE(model_id,name),
  INDEX idx_trim_model (model_id),
  INDEX idx_trim_engine (engine_id),
  CONSTRAINT fk_trim_model FOREIGN KEY (model_id) REFERENCES model(model_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_trim_engine FOREIGN KEY (engine_id) REFERENCES engine(engine_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE dealership (
  dealership_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  city VARCHAR(120) NOT NULL,
  country VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE customer (
  customer_id INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  email VARCHAR(200) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE orders (
  order_id INT AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  dealership_id INT NOT NULL,
  order_date DATE NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  INDEX idx_orders_customer (customer_id),
  INDEX idx_orders_dealership (dealership_id),
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_orders_dealership FOREIGN KEY (dealership_id) REFERENCES dealership(dealership_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE order_item (
  order_item_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  trim_id INT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL,
  INDEX idx_item_order (order_id),
  INDEX idx_item_trim (trim_id),
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_item_trim FOREIGN KEY (trim_id) REFERENCES `trim`(trim_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Table Data
INSERT INTO make (name) VALUES ('Porsche'), ('Ferrari'), ('Lamborghini');

INSERT INTO engine (code, cylinders, displacement_l, aspiration, horsepower, torque_nm) VALUES
('MA2.0T', 4, 2.0, 'Turbo', 300, 380),
('MA3.0T', 6, 3.0, 'Turbo', 450, 530),
('F154', 8, 3.9, 'Turbo', 710, 770),
('L539', 12, 6.5, 'NA', 770, 720);

INSERT INTO model (make_id, name, start_year) VALUES
(1,'911',1963),
(1,'718 Cayman',2016),
(2,'F8 Tributo',2019),
(3,'Aventador',2011);

INSERT INTO `trim` (model_id, name, msrp, engine_id, transmission, drivetrain) VALUES
(1,'Carrera S', 130000, 2, 'DCT', 'RWD'),
(1,'Turbo S', 210000, 2, 'DCT', 'AWD'),
(2,'GTS',  95000, 2, 'Manual', 'RWD'),
(3,'F8 Base', 275000, 3, 'DCT', 'RWD'),
(4,'SVJ', 517000, 4, 'Automatic', 'AWD');

INSERT INTO dealership (name, city, country) VALUES
('Downtown Motors','Munich','Germany'),
('Elite Performance','London','UK');

INSERT INTO customer (first_name, last_name, email) VALUES
('Alex','Morgan','alex.morgan@example.com'),
('Jamie','Lee','jamie.lee@example.com');

-- Sample order
INSERT INTO orders (customer_id, dealership_id, order_date, total_amount)
VALUES (1,1,'2025-01-12', 0), (2,2,'2025-02-10',0);

INSERT INTO order_item (order_id, trim_id, quantity, unit_price)
VALUES (1,1,1,130000.00), (1,3,1,95000.00), (2,4,1,275000.00);

-- Maintain order total
UPDATE orders o
JOIN (
  SELECT order_id, SUM(quantity * unit_price) AS sum_total
  FROM order_item
  GROUP BY order_id
) s ON s.order_id = o.order_id
SET o.total_amount = s.sum_total;

-- View: top models by sales revenue
CREATE OR REPLACE VIEW v_top_models AS
SELECT m.name AS model_name,
       mk.name AS make_name,
       SUM(oi.quantity * oi.unit_price) AS revenue
FROM order_item oi
JOIN `trim` t ON t.trim_id = oi.trim_id
JOIN model m ON m.model_id = t.model_id
JOIN make mk ON mk.make_id = m.make_id
GROUP BY m.model_id, mk.make_id
ORDER BY revenue DESC;

-- Stored procedure inserting order
DELIMITER //
CREATE PROCEDURE sp_create_order(
  IN p_customer_id INT,
  IN p_dealership_id INT,
  IN p_order_date DATE
)
BEGIN
  DECLARE v_order_id INT;
  START TRANSACTION;
  INSERT INTO orders(customer_id, dealership_id, order_date, total_amount)
  VALUES (p_customer_id, p_dealership_id, p_order_date, 0);
  SET v_order_id = LAST_INSERT_ID();
  COMMIT;
  SELECT v_order_id AS order_id;
END//
DELIMITER ;

-- View tables
SELECT * FROM make;
SELECT * FROM engine;
SELECT * FROM model;
SELECT * FROM `trim`;
SELECT * FROM dealership;
SELECT * FROM customer;
SELECT * FROM orders;
SELECT * FROM order_item;

-- View pre-built view
SELECT * FROM v_top_models;

-- Test stored procedure (creates new order & returns order_id)
CALL sp_create_order(1, 2, '2025-03-15');

-- Complete order details with all related info
SELECT 
    o.order_id,
    o.order_date,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    c.email AS customer_email,
    d.name AS dealership_name,
    d.city AS dealership_city,
    mk.name AS make_name,
    m.name AS model_name,
    t.name AS trim_name,
    t.transmission,
    t.drivetrain,
    e.code AS engine_code,
    e.horsepower,
    oi.quantity,
    oi.unit_price,
    (oi.quantity * oi.unit_price) AS line_total,
    o.total_amount AS order_total
FROM orders o
JOIN customer c ON c.customer_id = o.customer_id
JOIN dealership d ON d.dealership_id = o.dealership_id
JOIN order_item oi ON oi.order_id = o.order_id
JOIN `trim` t ON t.trim_id = oi.trim_id
JOIN model m ON m.model_id = t.model_id
JOIN make mk ON mk.make_id = m.make_id
JOIN engine e ON e.engine_id = t.engine_id
ORDER BY o.order_id, oi.order_item_id;
