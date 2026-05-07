-- BACP database bootstrap (MySQL 8.0)
CREATE DATABASE IF NOT EXISTS bacp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bacp;

SET NAMES utf8mb4;

-- ========== RBAC ==========
CREATE TABLE IF NOT EXISTS t_sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=active 0=disabled',
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    perm_code VARCHAR(128) NOT NULL,
    perm_name VARCHAR(128) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    perm_type VARCHAR(32) NOT NULL DEFAULT 'BUTTON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES t_sys_role (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    KEY idx_perm (perm_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES t_sys_role (id),
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES t_sys_permission (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    params TEXT,
    ip VARCHAR(64) DEFAULT NULL,
    success TINYINT NOT NULL DEFAULT 1,
    error_msg VARCHAR(512) DEFAULT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_created (created_at),
    KEY idx_user (user_id)
) ENGINE=InnoDB;

-- ========== System config ==========
CREATE TABLE IF NOT EXISTS t_currency (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    asset_type VARCHAR(32) NOT NULL DEFAULT 'NATIVE',
    chain_type VARCHAR(32) NOT NULL,
    contract_address VARCHAR(128) DEFAULT NULL,
    decimals INT NOT NULL DEFAULT 18,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_symbol_chain (symbol, chain_type)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_chain_node (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chain_type VARCHAR(32) NOT NULL,
    node_name VARCHAR(128) NOT NULL,
    rpc_url VARCHAR(512) NOT NULL,
    ws_url VARCHAR(512) DEFAULT NULL,
    priority INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_chain_priority (chain_type, priority)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_sys_param (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    param_key VARCHAR(128) NOT NULL,
    param_value VARCHAR(1024) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB;

-- ========== Custody ==========
CREATE TABLE IF NOT EXISTS t_wallet (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chain_type VARCHAR(32) NOT NULL,
    address VARCHAR(128) NOT NULL,
    encrypted_private_key TEXT NOT NULL,
    derivation_path VARCHAR(128) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_chain (user_id, chain_type),
    KEY idx_address (address),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_balance (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency_id BIGINT NOT NULL,
    available_amount DECIMAL(38,18) NOT NULL DEFAULT 0,
    frozen_amount DECIMAL(38,18) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_currency (user_id, currency_id),
    CONSTRAINT fk_bal_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id),
    CONSTRAINT fk_bal_currency FOREIGN KEY (currency_id) REFERENCES t_currency (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_tx (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency_id BIGINT NOT NULL,
    chain_type VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    tx_hash VARCHAR(128) DEFAULT NULL,
    from_address VARCHAR(128) DEFAULT NULL,
    to_address VARCHAR(128) DEFAULT NULL,
    amount DECIMAL(38,18) NOT NULL,
    fee DECIMAL(38,18) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    confirmations INT NOT NULL DEFAULT 0,
    block_number BIGINT DEFAULT NULL,
    remark VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tx_hash (tx_hash),
    KEY idx_user_created (user_id, created_at),
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id),
    CONSTRAINT fk_tx_currency FOREIGN KEY (currency_id) REFERENCES t_currency (id)
) ENGINE=InnoDB;

-- ========== Trade ==========
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    side VARCHAR(16) NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    price DECIMAL(38,18) DEFAULT NULL,
    quantity DECIMAL(38,18) NOT NULL,
    filled_quantity DECIMAL(38,18) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_symbol_status (symbol, status),
    CONSTRAINT fk_ord_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_trade (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(64) NOT NULL,
    buy_order_id BIGINT NOT NULL,
    sell_order_id BIGINT NOT NULL,
    price DECIMAL(38,18) NOT NULL,
    quantity DECIMAL(38,18) NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_symbol_created (symbol, created_at),
    CONSTRAINT fk_tr_buy FOREIGN KEY (buy_order_id) REFERENCES t_order (id),
    CONSTRAINT fk_tr_sell FOREIGN KEY (sell_order_id) REFERENCES t_order (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_capital_flow (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency_id BIGINT NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount DECIMAL(38,18) NOT NULL,
    balance_before DECIMAL(38,18) NOT NULL,
    balance_after DECIMAL(38,18) NOT NULL,
    ref_type VARCHAR(64) DEFAULT NULL,
    ref_id BIGINT DEFAULT NULL,
    remark VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_user_created (user_id, created_at),
    CONSTRAINT fk_cf_user FOREIGN KEY (user_id) REFERENCES t_sys_user (id),
    CONSTRAINT fk_cf_currency FOREIGN KEY (currency_id) REFERENCES t_currency (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_risk_alert (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    user_id BIGINT DEFAULT NULL,
    payload_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_rule_created (rule_code, created_at)
) ENGINE=InnoDB;

-- ========== Seed roles ==========
INSERT INTO t_sys_role (role_code, role_name, deleted) VALUES
('SUPER_ADMIN', 'Super Administrator', 0),
('OPS', 'Operations', 0),
('FINANCE', 'Finance', 0),
('USER', 'Standard User', 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- ========== Seed permissions (subset; SUPER_ADMIN also gets wildcard in code) ==========
INSERT INTO t_sys_permission (perm_code, perm_name, parent_id, perm_type, deleted) VALUES
('user:create', 'Create user', 0, 'BUTTON', 0),
('user:update', 'Update user', 0, 'BUTTON', 0),
('user:delete', 'Delete user', 0, 'BUTTON', 0),
('user:query', 'Query users', 0, 'BUTTON', 0),
('role:create', 'Create role', 0, 'BUTTON', 0),
('role:update', 'Update role', 0, 'BUTTON', 0),
('role:delete', 'Delete role', 0, 'BUTTON', 0),
('role:query', 'Query roles', 0, 'BUTTON', 0),
('perm:query', 'Query permissions', 0, 'BUTTON', 0),
('oplog:query', 'Query operation logs', 0, 'BUTTON', 0),
('currency:create', 'Create currency', 0, 'BUTTON', 0),
('currency:update', 'Update currency', 0, 'BUTTON', 0),
('currency:delete', 'Delete currency', 0, 'BUTTON', 0),
('currency:query', 'Query currencies', 0, 'BUTTON', 0),
('node:create', 'Create chain node', 0, 'BUTTON', 0),
('node:update', 'Update chain node', 0, 'BUTTON', 0),
('node:query', 'Query chain nodes', 0, 'BUTTON', 0),
('param:update', 'Update system params', 0, 'BUTTON', 0),
('dashboard:view', 'View dashboard', 0, 'BUTTON', 0),
('monitor:view', 'View monitor APIs', 0, 'BUTTON', 0),
('custody:deposit:notify', 'Notify deposit', 0, 'BUTTON', 0),
('custody:withdraw', 'Withdraw', 0, 'BUTTON', 0),
('trade:order', 'Place order', 0, 'BUTTON', 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

-- Bind all permissions to SUPER_ADMIN role (id=1)
INSERT IGNORE INTO t_sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM t_sys_role r CROSS JOIN t_sys_permission p WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO t_sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM t_sys_role r JOIN t_sys_permission p ON p.perm_code IN ('trade:order','custody:withdraw') WHERE r.role_code = 'USER';

-- ========== Seed currencies ==========
INSERT INTO t_currency (symbol, name, asset_type, chain_type, contract_address, decimals, enabled, deleted) VALUES
('ETH', 'Ether', 'NATIVE', 'ethereum', NULL, 18, 1, 0),
('USDT', 'Tether USD', 'ERC20', 'ethereum', '0x0000000000000000000000000000000000000001', 6, 1, 0),
('MATIC', 'Polygon', 'NATIVE', 'polygon', NULL, 18, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ========== Seed chain nodes (testnet placeholders; replace in prod) ==========
INSERT INTO t_chain_node (chain_type, node_name, rpc_url, ws_url, priority, enabled, deleted) VALUES
('ethereum', 'Sepolia public', 'https://ethereum-sepolia.publicnode.com', '', 10, 1, 0),
('bsc', 'BSC testnet', 'https://data-seed-prebsc-1-s1.binance.org:8545', '', 10, 1, 0),
('polygon', 'Amoy public', 'https://rpc-amoy.polygon.technology', '', 10, 1, 0)
ON DUPLICATE KEY UPDATE rpc_url = VALUES(rpc_url);

-- ========== Seed system params ==========
INSERT INTO t_sys_param (param_key, param_value, remark, deleted) VALUES
('fee.trade.rate', '0.001', 'Trading fee rate', 0),
('fee.withdraw.fixed', '0.0005', 'Flat withdraw fee (native units)', 0),
('withdraw.daily.limit', '50000', 'Daily withdraw limit per user (USD equiv)', 0),
('withdraw.single.min', '0.0001', 'Minimum single withdraw', 0),
('withdraw.single.max', '1000', 'Maximum single withdraw', 0)
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);
