-- 创建budgets表
CREATE TABLE IF NOT EXISTS budgets (
  id int NOT NULL AUTO_INCREMENT,
  budget_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  amount decimal(18,2) NOT NULL,
  project_id int DEFAULT NULL,
  contract_id int DEFAULT NULL,
  start_date date DEFAULT NULL,
  end_date date DEFAULT NULL,
  status varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY project_id (project_id),
  KEY contract_id (contract_id),
  CONSTRAINT budgets_ibfk_1 FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT budgets_ibfk_2 FOREIGN KEY (contract_id) REFERENCES subcontractor_contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;