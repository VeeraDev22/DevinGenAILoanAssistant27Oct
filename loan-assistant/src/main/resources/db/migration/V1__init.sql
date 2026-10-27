CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    role VARCHAR(32) NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE loan_application (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    applicant_name VARCHAR(128) NOT NULL,
    applicant_email VARCHAR(128) NOT NULL,
    annual_income DECIMAL(19,2) NOT NULL,
    monthly_debt_payments DECIMAL(19,2) NOT NULL,
    credit_score INT NOT NULL,
    employment_status VARCHAR(32) NOT NULL,
    employer_name VARCHAR(128),
    months_employed INT NOT NULL,
    loan_amount DECIMAL(19,2) NOT NULL,
    term_months INT NOT NULL,
    loan_purpose VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_application_customer FOREIGN KEY (customer_id) REFERENCES users (id)
);

CREATE INDEX idx_application_status ON loan_application (status);
CREATE INDEX idx_application_customer ON loan_application (customer_id);

CREATE TABLE loan_document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    extracted_text LONGTEXT,
    uploaded_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_document_application FOREIGN KEY (application_id) REFERENCES loan_application (id)
);

CREATE TABLE ai_analysis_result (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    summary LONGTEXT,
    missing_information LONGTEXT,
    inconsistencies LONGTEXT,
    risk_factors LONGTEXT,
    extracted_data_json LONGTEXT,
    model_used VARCHAR(128),
    fallback_used BIT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_analysis_application UNIQUE (application_id),
    CONSTRAINT fk_analysis_application FOREIGN KEY (application_id) REFERENCES loan_application (id)
);

CREATE TABLE rule_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    threshold DECIMAL(19,4),
    actual_value DECIMAL(19,4),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ruleeval_application FOREIGN KEY (application_id) REFERENCES loan_application (id)
);

CREATE TABLE decision_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    decision VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    decided_by VARCHAR(64) NOT NULL,
    rationale LONGTEXT,
    customer_explanation LONGTEXT,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_decision_application FOREIGN KEY (application_id) REFERENCES loan_application (id)
);

CREATE TABLE customer_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body LONGTEXT,
    read_flag BIT NOT NULL,
    sent_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_notification_application FOREIGN KEY (application_id) REFERENCES loan_application (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);
