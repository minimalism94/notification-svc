DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notification_preference;

CREATE TABLE notification_preference (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    type VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    contact_info VARCHAR(255),
    created_on TIMESTAMP NOT NULL,
    updated_on TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_user_id UNIQUE (user_id)
);

CREATE TABLE notifications (
    id BINARY(16) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body VARCHAR(255) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    user_id BINARY(16),
    deleted BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

