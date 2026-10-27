-- ============================================
-- CEAMS Database Initialization
-- ============================================

-- Create database
CREATE DATABASE IF NOT EXISTS ceams DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ceams;

-- ============================================
-- Tables from existing structure
-- ============================================

-- Users table
create table users
(
    user_id       int auto_increment
        primary key,
    username      varchar(50)                           not null,
    password_hash varchar(255)                          not null,
    role          varchar(20) default 'EMPLOYEE'        not null comment '用户角色',
    name          varchar(100)                          not null,
    email         varchar(100)                          null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

-- Projects table
create table projects
(
    project_id     int auto_increment
        primary key,
    project_name   varchar(200)                          not null,
    project_code   varchar(50)                           null,
    location       varchar(200)                          null,
    start_date     date                                  null,
    end_date       date                                  null,
    total_budget   decimal(18, 2)                        null,
    manager_id     int                                   null,
    status         varchar(50) default 'PLANNING'        not null,
    description    text                                  null,
    created_at     datetime    default CURRENT_TIMESTAMP not null,
    updated_at     datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    index idx_project_code (project_code),
    index idx_manager (manager_id),
    index idx_status (status),
    constraint fk_project_manager
        foreign key (manager_id) references users (user_id)
            on delete set null
);

-- Subcontractors table
create table subcontractors
(
    subcontractor_id int auto_increment
        primary key,
    company_name     varchar(200)                          not null,
    contact_name     varchar(100)                          null,
    contact_phone    varchar(20)                           null,
    contact_email    varchar(100)                          null,
    address          varchar(200)                          null,
    tax_number       varchar(50)                           null,
    status           varchar(20) default 'ACTIVE'          not null,
    created_at       datetime    default CURRENT_TIMESTAMP not null,
    updated_at       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

-- Materials table
create table materials
(
    material_id  int auto_increment
        primary key,
    material_name varchar(200)                          not null,
    material_code varchar(50)                           null,
    category      varchar(100)                          null,
    unit          varchar(20)                           null,
    price         decimal(18, 2)                        null,
    specification varchar(200)                          null,
    status        varchar(20) default 'ACTIVE'          not null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    index idx_material_code (material_code),
    index idx_category (category)
);

-- Accounts table
create table accounts
(
    account_id    int auto_increment
        primary key,
    account_code  varchar(50)                           null,
    account_name  varchar(200)                          not null,
    type          varchar(50)                           null comment 'INCOME/EXPENSE',
    category      varchar(100)                          null,
    project_id    int                                   null,
    amount        decimal(18, 2)                        null,
    date          date                                  null,
    description   text                                  null,
    status        varchar(20) default 'PENDING'         not null,
    created_by    int                                   null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    index idx_account_code (account_code),
    index idx_project_id (project_id),
    index idx_type (type),
    index idx_status (status),
    constraint fk_account_project
        foreign key (project_id) references projects (project_id)
            on delete set null,
    constraint fk_account_creator
        foreign key (created_by) references users (user_id)
            on delete set null
);

-- Receipts table
create table receipts
(
    receipt_id      int auto_increment
        primary key,
    receipt_code    varchar(50)                           null,
    type            varchar(50)                           null,
    amount          decimal(18, 2)                        null,
    amount_in_words varchar(200)                          null,
    date            date                                  null,
    description     text                                  null,
    project_id      int                                   null,
    supplier_id     int                                   null,
    file_path       varchar(500)                          null,
    file_original_name varchar(200)                      null,
    status          varchar(20) default 'PENDING'         not null,
    created_by      int                                   null,
    created_at      datetime    default CURRENT_TIMESTAMP not null,
    updated_at      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    index idx_receipt_code (receipt_code),
    index idx_project_id (project_id),
    index idx_status (status),
    constraint fk_receipt_project
        foreign key (project_id) references projects (project_id)
            on delete set null,
    constraint fk_receipt_supplier
        foreign key (supplier_id) references subcontractors (subcontractor_id)
            on delete set null,
    constraint fk_receipt_creator
        foreign key (created_by) references users (user_id)
            on delete set null
);

-- Approval record table
create table approval_record
(
    record_id     int auto_increment
        primary key,
    receipt_id    int                                   null,
    account_id    int                                   null,
    type          varchar(50)                           null,
    step          int                                   null,
    approver_id   int                                   null,
    action        varchar(50)                           null,
    comment       text                                  null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    index idx_receipt_id (receipt_id),
    index idx_account_id (account_id),
    index idx_approver_id (approver_id),
    constraint fk_approval_receipt
        foreign key (receipt_id) references receipts (receipt_id)
            on delete cascade,
    constraint fk_approval_account
        foreign key (account_id) references accounts (account_id)
            on delete cascade,
    constraint fk_approval_approver
        foreign key (approver_id) references users (user_id)
            on delete set null
);

-- Electronic seals table
create table electronic_seals
(
    seal_id      int auto_increment
        primary key,
    seal_name    varchar(200)                          not null,
    seal_code    varchar(50)                           null,
    type         varchar(50)                           null,
    status       varchar(20) default 'ACTIVE'          not null,
    creator_id   int                                   null,
    created_at   datetime    default CURRENT_TIMESTAMP not null,
    updated_at   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    index idx_seal_code (seal_code),
    index idx_creator_id (creator_id),
    constraint fk_seal_creator
        foreign key (creator_id) references users (user_id)
            on delete set null
);

-- Operation logs table
create table operation_logs
(
    log_id    int auto_increment
        primary key,
    user_id   int                                   null,
    username  varchar(50)                           null,
    action    varchar(200)                          null,
    module    varchar(100)                          null,
    details   text                                  null,
    ip_address varchar(50)                           null,
    created_at datetime default CURRENT_TIMESTAMP    not null,
    index idx_user_id (user_id),
    index idx_created_at (created_at),
    constraint fk_log_user
        foreign key (user_id) references users (user_id)
            on delete set null
);

-- ============================================
-- Other tables (simplified)
-- ============================================

-- Add other tables here as needed
create table budgets
(
    budget_id  int auto_increment primary key,
    project_id int,
    amount     decimal(18, 2),
    created_at datetime default CURRENT_TIMESTAMP
);

create table payment_account
(
    account_id int auto_increment primary key,
    name       varchar(100),
    balance    decimal(18, 2),
    created_at datetime default CURRENT_TIMESTAMP
);

-- ============================================
-- Insert default data
-- ============================================

-- Insert admin user (password: admin123)
INSERT INTO users (username, password_hash, role, name, email) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6zXyO', 'ADMIN', 'Administrator', 'admin@example.com');

-- Insert manager user (password: manager123)
INSERT INTO users (username, password_hash, role, name, email) VALUES
('manager', '$2a$10$U0q8qV7y3fV2Jx9LkG6vSejX2dZ9h8Z7y3X2W1eQ1aW9j8K8L0aK', 'MANAGER', 'Manager', 'manager@example.com');

-- ============================================
-- Complete
-- ============================================
SELECT 'Database initialization complete!' AS message;
