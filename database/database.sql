-- PlaceSure 1.0 | MySQL 8.0+ / MariaDB 10.6+
-- Fictional demonstration data only. No credentials are stored here.
-- Create a database and an application user separately; import into that database.
-- Example: mysql -u root -p placesure < database/database.sql
-- This script creates an EMPTY database's tables. It deliberately does not DROP data.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  register_number VARCHAR(32) NOT NULL UNIQUE,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  department VARCHAR(30) NOT NULL,
  graduation_year SMALLINT NOT NULL,
  cgpa DECIMAL(4,2) NOT NULL,
  active_arrears SMALLINT NOT NULL DEFAULT 0,
  history_arrears SMALLINT NOT NULL DEFAULT 0,
  phone VARCHAR(24) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_student_cgpa CHECK (cgpa BETWEEN 0 AND 10),
  CONSTRAINT chk_student_active CHECK (active_arrears >= 0),
  CONSTRAINT chk_student_history CHECK (history_arrears >= 0),
  CONSTRAINT chk_student_year CHECK (graduation_year BETWEEN 2000 AND 2100),
  INDEX idx_students_department (department), INDEX idx_students_name (full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS companies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  job_role VARCHAR(120) NOT NULL,
  drive_date DATE NULL,
  salary_package VARCHAR(80) NULL,
  notes VARCHAR(2000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_companies_drive (drive_date), INDEX idx_companies_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS company_criteria (
  company_id BIGINT PRIMARY KEY,
  min_cgpa DECIMAL(4,2) NOT NULL,
  max_active_arrears SMALLINT NOT NULL,
  max_history_arrears SMALLINT NULL COMMENT 'NULL = historical arrears are not considered',
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT chk_min_cgpa CHECK (min_cgpa BETWEEN 0 AND 10),
  CONSTRAINT chk_max_active CHECK (max_active_arrears >= 0),
  CONSTRAINT chk_max_history CHECK (max_history_arrears IS NULL OR max_history_arrears >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skills (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_skills (
  student_id BIGINT NOT NULL, skill_id BIGINT NOT NULL,
  PRIMARY KEY (student_id, skill_id),
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS company_required_skills (
  company_id BIGINT NOT NULL, skill_id BIGINT NOT NULL,
  PRIMARY KEY (company_id, skill_id),
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS company_departments (
  company_id BIGINT NOT NULL, department VARCHAR(30) NOT NULL,
  PRIMARY KEY (company_id, department),
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS company_graduation_years (
  company_id BIGINT NOT NULL, graduation_year SMALLINT NOT NULL,
  PRIMARY KEY (company_id, graduation_year),
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT chk_company_year CHECK (graduation_year BETWEEN 2000 AND 2100)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NULL, company_id BIGINT NULL,
  student_source_id BIGINT NOT NULL, company_source_id BIGINT NOT NULL,
  student_name VARCHAR(120) NOT NULL, register_number VARCHAR(32) NOT NULL,
  department VARCHAR(30) NOT NULL, company_name VARCHAR(120) NOT NULL, job_role VARCHAR(120) NOT NULL,
  student_snapshot JSON NOT NULL, company_snapshot JSON NOT NULL,
  eligible BOOLEAN NOT NULL, passed_count SMALLINT NOT NULL, failed_count SMALLINT NOT NULL,
  evaluated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE SET NULL,
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
  CONSTRAINT chk_evaluation_result CHECK ((eligible = 1 AND failed_count = 0) OR (eligible = 0 AND failed_count > 0)),
  INDEX idx_evaluation_time (evaluated_at),
  INDEX idx_evaluation_student_company (student_source_id, company_source_id, id),
  INDEX idx_evaluation_company_status (company_source_id, eligible),
  INDEX idx_evaluation_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evaluation_details (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  evaluation_id BIGINT NOT NULL,
  criterion_key VARCHAR(64) NOT NULL, criterion VARCHAR(80) NOT NULL,
  requirement_snapshot TEXT NOT NULL, actual_snapshot TEXT NOT NULL,
  passed BOOLEAN NOT NULL, explanation TEXT NOT NULL, position SMALLINT NOT NULL,
  FOREIGN KEY (evaluation_id) REFERENCES evaluations(id) ON DELETE CASCADE,
  UNIQUE KEY uq_evaluation_criterion (evaluation_id, criterion_key),
  INDEX idx_details_failure (passed, criterion_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Explicit IDs make the demo easy to explain and reproduce. Import once into a new database.
INSERT INTO skills(id,name) VALUES
(1,'Java'),(2,'SQL'),(3,'Git'),(4,'Python'),(5,'JavaScript'),(6,'HTML'),(7,'CSS'),
(8,'Linux'),(9,'Networking'),(10,'C'),(11,'C++'),(12,'Excel'),(13,'React'),(14,'Communication');

INSERT INTO students(id,register_number,full_name,email,department,graduation_year,cgpa,active_arrears,history_arrears,phone) VALUES
(1,'PS27CS001','Aarav Sharma','aarav.sharma@example.test','CSE',2027,8.72,0,0,NULL),
(2,'PS27IT002','Diya Menon','diya.menon@example.test','IT',2027,9.14,0,0,NULL),
(3,'PS27EC003','Ishaan Rao','ishaan.rao@example.test','ECE',2027,7.85,0,1,NULL),
(4,'PS27CS004','Ananya Reddy','ananya.reddy@example.test','CSE',2027,8.40,0,0,NULL),
(5,'PS27IT005','Kavya Nair','kavya.nair@example.test','IT',2027,6.80,1,2,NULL),
(6,'PS27CS006','Arjun Mehta','arjun.mehta@example.test','CSE',2027,7.00,0,2,NULL),
(7,'PS27EC007','Meera Iyer','meera.iyer@example.test','ECE',2027,8.95,0,0,NULL),
(8,'PS27EE008','Rohan Das','rohan.das@example.test','EEE',2027,6.25,2,3,NULL),
(9,'PS27CS009','Nisha Patel','nisha.patel@example.test','CSE',2027,9.36,0,0,NULL),
(10,'PS27ME010','Aditya Verma','aditya.verma@example.test','MECH',2027,7.60,0,1,NULL),
(11,'PS27IT011','Sara Khan','sara.khan@example.test','IT',2027,7.45,1,1,NULL),
(12,'PS26CS012','Vikram Sen','vikram.sen@example.test','CSE',2026,8.10,0,0,NULL),
(13,'PS27EC013','Priya Joshi','priya.joshi@example.test','ECE',2027,7.92,0,0,NULL),
(14,'PS27CS014','Dev Kapoor','dev.kapoor@example.test','CSE',2027,5.90,3,4,NULL),
(15,'PS27IT015','Tara Bansal','tara.bansal@example.test','IT',2027,8.56,0,0,NULL),
(16,'PS27EE016','Neel Desai','neel.desai@example.test','EEE',2027,7.15,0,1,NULL),
(17,'PS28EC017','Sana Ali','sana.ali@example.test','ECE',2028,8.22,0,0,NULL),
(18,'PS27CS018','Kiran Sethi','kiran.sethi@example.test','CSE',2027,7.68,0,2,NULL);

INSERT INTO student_skills(student_id,skill_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,8),(1,9),
(2,4),(2,2),(2,12),(2,5),(2,6),(2,7),(2,1),(2,3),
(3,10),(3,11),(3,1),(3,2),(3,3),(3,8),(3,9),
(4,1),(4,2),(4,3),(4,5),(4,6),(4,7),(4,13),
(5,5),(5,6),(5,7),(5,8),(5,9),
(6,1),(6,2),(6,3),(6,10),(6,11),
(7,10),(7,11),(7,4),(7,2),(7,8),(7,9),
(8,10),(8,8),(8,9),
(9,1),(9,2),(9,3),(9,4),(9,5),(9,6),(9,7),
(10,10),(10,12),(10,14),(10,8),(10,9),
(11,4),(11,2),(11,5),(11,6),(11,7),
(12,1),(12,2),(12,3),(12,8),(12,9),
(13,10),(13,11),(13,4),(13,2),
(14,6),(14,7),
(15,4),(15,2),(15,5),(15,6),(15,7),(15,1),(15,3),
(16,10),(16,11),(16,8),(16,9),
(17,4),(17,2),(17,10),(17,11),
(18,1),(18,2),(18,3),(18,5),(18,6),(18,7);

INSERT INTO companies(id,name,job_role,drive_date,salary_package,notes) VALUES
(1,'NovaTech','Software Engineer',DATE_ADD(CURRENT_DATE,INTERVAL 10 DAY),'8–12 LPA','Strong problem-solving and communication skills are appreciated. Notes are advisory, not an automatically evaluated criterion.'),
(2,'Vertex Digital','Full Stack Developer',DATE_ADD(CURRENT_DATE,INTERVAL 6 DAY),'6–8 LPA','Bring a portfolio of web projects. All required technical skills must be present.'),
(3,'Aether Labs','Data Analyst',DATE_ADD(CURRENT_DATE,INTERVAL 17 DAY),'9–12 LPA','Historical arrears are considered: no prior arrears are permitted.'),
(4,'Cloudline Systems','Cloud Associate',DATE_ADD(CURRENT_DATE,INTERVAL 24 DAY),'5–7 LPA','Open to all departments. Historical arrears are not considered.'),
(5,'Nexora','Embedded Engineer',DATE_ADD(CURRENT_DATE,INTERVAL 13 DAY),'7–10 LPA','C and C++ are two distinct required skills.');

INSERT INTO company_criteria(company_id,min_cgpa,max_active_arrears,max_history_arrears) VALUES
(1,7.00,0,2),(2,6.50,1,NULL),(3,7.50,0,0),(4,6.00,2,NULL),(5,7.00,0,1);
INSERT INTO company_required_skills(company_id,skill_id) VALUES
(1,1),(1,2),(1,3),(2,5),(2,6),(2,7),(3,4),(3,2),(4,8),(4,9),(5,10),(5,11);
INSERT INTO company_departments(company_id,department) VALUES
(1,'CSE'),(1,'IT'),(1,'ECE'),(2,'CSE'),(2,'IT'),(3,'CSE'),(3,'IT'),(3,'ECE'),(5,'ECE'),(5,'EEE'),(5,'CSE');
INSERT INTO company_graduation_years(company_id,graduation_year) VALUES
(1,2026),(1,2027),(2,2027),(3,2027),(4,2026),(4,2027),(5,2027);
-- Evaluations are intentionally not fabricated in SQL. Run the Java app with --seed-demo
-- to compute and persist 40 demonstration evaluations using EligibilityService.
