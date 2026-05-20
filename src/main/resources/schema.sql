CREATE TABLE IF NOT EXISTS users
(
    user_id          VARCHAR(50)  NOT NULL,
    password         VARCHAR(255) NOT NULL,
    pass_expire_date CHAR(8)      NOT NULL,
    delete_flag      CHAR(1)      NOT NULL DEFAULT 'N',
    creator_id       VARCHAR(50)  NOT NULL,
    create_dttm      CHAR(14)     NOT NULL,
    modifier_id      VARCHAR(50),
    modify_dttm      CHAR(14),

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT chk_delete_flag CHECK (delete_flag IN ('Y', 'N'))
);
