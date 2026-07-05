CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles   VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);
