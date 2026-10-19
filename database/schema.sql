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


-- Schema-only import: no fictional profiles, companies, or evaluations.
