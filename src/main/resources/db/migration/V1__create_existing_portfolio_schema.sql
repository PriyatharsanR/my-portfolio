CREATE TABLE admin (
    id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_username UNIQUE (username)
) ENGINE=InnoDB;

CREATE TABLE certificates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255),
    url VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE contact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(255),
    url VARCHAR(255),
    message VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE experiences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company VARCHAR(255),
    position VARCHAR(255),
    start_date DATE,
    end_date DATE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_name VARCHAR(255),
    description VARCHAR(2000),
    technology VARCHAR(255),
    github_url VARCHAR(255),
    live_url VARCHAR(255),
    created_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    level VARCHAR(255),
    category VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
