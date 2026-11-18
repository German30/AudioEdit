-- Baseline schema for the Auralis audio platform.
-- Prod runs with Flyway + spring.jpa.hibernate.ddl-auto=validate.
-- Dev uses ddl-auto=update instead (Flyway disabled), so this file is the
-- production source of truth and is authored to match the JPA entities.
-- UUIDs are stored as CHAR(36) (hibernate.type.preferred_uuid_jdbc_type=CHAR).

CREATE TABLE users (
    id           CHAR(36)      NOT NULL PRIMARY KEY,
    version      BIGINT        NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    email        VARCHAR(255)  NOT NULL,
    display_name VARCHAR(255)  NOT NULL,
    avatar_url   VARCHAR(255)  NULL,
    provider     VARCHAR(16)   NOT NULL,
    provider_id  VARCHAR(255)  NULL,
    role         VARCHAR(16)   NOT NULL,
    enabled      BIT(1)        NOT NULL,
    last_login_at DATETIME(6)  NULL,
    CONSTRAINT ux_users_email UNIQUE (email)
);
CREATE INDEX ix_users_provider ON users (provider, provider_id);

CREATE TABLE refresh_tokens (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    version     BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BIT(1)       NOT NULL,
    replaced_by CHAR(36)     NULL,
    user_agent  VARCHAR(128) NULL,
    CONSTRAINT ux_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_user ON refresh_tokens (user_id);

CREATE TABLE plans (
    id                      CHAR(36)     NOT NULL PRIMARY KEY,
    version                 BIGINT       NOT NULL,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    tier                    VARCHAR(16)  NOT NULL,
    name                    VARCHAR(80)  NOT NULL,
    description             VARCHAR(500) NULL,
    price_cents             BIGINT       NOT NULL,
    currency                VARCHAR(3)   NOT NULL,
    active                  BIT(1)       NOT NULL,
    sort_order              INTEGER      NOT NULL,
    max_projects            INTEGER      NOT NULL,
    max_collaborators       INTEGER      NOT NULL,
    monthly_render_minutes  INTEGER      NOT NULL,
    max_storage_bytes       BIGINT       NOT NULL,
    max_upload_bytes        BIGINT       NOT NULL,
    allowed_export_formats  VARCHAR(255) NOT NULL,
    allow_stem_export       BIT(1)       NOT NULL,
    max_export_bitrate_kbps INTEGER      NOT NULL,
    history_retention_days  INTEGER      NOT NULL,
    CONSTRAINT ux_plans_tier UNIQUE (tier)
);

CREATE TABLE subscriptions (
    id                       CHAR(36)     NOT NULL PRIMARY KEY,
    version                  BIGINT       NOT NULL,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    user_id                  CHAR(36)     NOT NULL,
    tier                     VARCHAR(16)  NOT NULL,
    status                   VARCHAR(16)  NOT NULL,
    current_period_start     DATETIME(6)  NOT NULL,
    current_period_end       DATETIME(6)  NULL,
    cancel_at_period_end     BIT(1)       NOT NULL,
    external_customer_id     VARCHAR(100) NULL,
    external_subscription_id VARCHAR(100) NULL,
    CONSTRAINT ux_subscriptions_user UNIQUE (user_id)
);
CREATE INDEX ix_subscriptions_external ON subscriptions (external_subscription_id);

CREATE TABLE usage_counters (
    id                  CHAR(36)    NOT NULL PRIMARY KEY,
    version             BIGINT      NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    user_id             CHAR(36)    NOT NULL,
    period_month        DATE        NOT NULL,
    render_seconds_used BIGINT      NOT NULL,
    storage_bytes_used  BIGINT      NOT NULL,
    CONSTRAINT ux_usage_user UNIQUE (user_id)
);

CREATE TABLE audio_assets (
    id                CHAR(36)     NOT NULL PRIMARY KEY,
    version           BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    owner_user_id     CHAR(36)     NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    duration_seconds  DOUBLE       NULL,
    sample_rate       INTEGER      NULL,
    channels          INTEGER      NULL,
    codec             VARCHAR(40)  NULL,
    bit_rate_kbps     INTEGER      NULL,
    waveform_key      VARCHAR(512) NULL,
    failure_reason    VARCHAR(500) NULL
);
CREATE INDEX ix_assets_owner ON audio_assets (owner_user_id);
CREATE INDEX ix_assets_status ON audio_assets (status);

CREATE TABLE projects (
    id             CHAR(36)      NOT NULL PRIMARY KEY,
    version        BIGINT        NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    owner_user_id  CHAR(36)      NOT NULL,
    name           VARCHAR(140)  NOT NULL,
    description    VARCHAR(1000) NULL,
    sample_rate    INTEGER       NOT NULL,
    tempo_bpm      DOUBLE        NULL,
    master_gain_db DOUBLE        NOT NULL,
    archived       BIT(1)        NOT NULL,
    last_opened_at DATETIME(6)   NULL
);
CREATE INDEX ix_projects_owner ON projects (owner_user_id);

CREATE TABLE project_members (
    id         CHAR(36)    NOT NULL PRIMARY KEY,
    version    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    role       VARCHAR(16) NOT NULL,
    CONSTRAINT ux_project_member UNIQUE (project_id, user_id)
);
CREATE INDEX ix_project_member_user ON project_members (user_id);

CREATE TABLE tracks (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    project_id CHAR(36)     NOT NULL,
    name       VARCHAR(120) NOT NULL,
    position   INTEGER      NOT NULL,
    gain_db    DOUBLE       NOT NULL,
    pan        DOUBLE       NOT NULL,
    muted      BIT(1)       NOT NULL,
    soloed     BIT(1)       NOT NULL,
    color      VARCHAR(9)   NULL
);
CREATE INDEX ix_tracks_project ON tracks (project_id);

CREATE TABLE clips (
    id                     CHAR(36)     NOT NULL PRIMARY KEY,
    version                BIGINT       NOT NULL,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    track_id               CHAR(36)     NOT NULL,
    project_id             CHAR(36)     NOT NULL,
    source_asset_id        CHAR(36)     NOT NULL,
    name                   VARCHAR(140) NULL,
    timeline_start_seconds DOUBLE       NOT NULL,
    source_offset_seconds  DOUBLE       NOT NULL,
    duration_seconds       DOUBLE       NOT NULL,
    gain_db                DOUBLE       NOT NULL,
    fade_in_seconds        DOUBLE       NOT NULL,
    fade_out_seconds       DOUBLE       NOT NULL,
    playback_rate          DOUBLE       NOT NULL,
    reversed               BIT(1)       NOT NULL
);
CREATE INDEX ix_clips_track ON clips (track_id);
CREATE INDEX ix_clips_asset ON clips (source_asset_id);

CREATE TABLE effects (
    id          CHAR(36)    NOT NULL PRIMARY KEY,
    version     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    project_id  CHAR(36)    NOT NULL,
    scope       VARCHAR(16) NOT NULL,
    clip_id     CHAR(36)    NULL,
    track_id    CHAR(36)    NULL,
    type        VARCHAR(20) NOT NULL,
    position    INTEGER     NOT NULL,
    enabled     BIT(1)      NOT NULL,
    params_json TEXT        NOT NULL
);
CREATE INDEX ix_effects_project ON effects (project_id);
CREATE INDEX ix_effects_clip ON effects (clip_id);
CREATE INDEX ix_effects_track ON effects (track_id);

CREATE TABLE edit_versions (
    id             CHAR(36)     NOT NULL PRIMARY KEY,
    version        BIGINT       NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    project_id     CHAR(36)     NOT NULL,
    author_user_id CHAR(36)     NOT NULL,
    label          VARCHAR(200) NULL,
    revision       BIGINT       NOT NULL,
    graph_json     LONGTEXT     NOT NULL
);
CREATE INDEX ix_versions_project ON edit_versions (project_id, created_at);

CREATE TABLE render_jobs (
    id                   CHAR(36)      NOT NULL PRIMARY KEY,
    version              BIGINT        NOT NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6)   NOT NULL,
    project_id           CHAR(36)      NOT NULL,
    edit_version_id      CHAR(36)      NOT NULL,
    requested_by_user_id CHAR(36)      NOT NULL,
    status               VARCHAR(12)   NOT NULL,
    format               VARCHAR(8)    NOT NULL,
    bitrate_kbps         INTEGER       NOT NULL,
    sample_rate          INTEGER       NOT NULL,
    include_stems        BIT(1)        NOT NULL,
    output_object_key    VARCHAR(512)  NULL,
    stems_object_key     VARCHAR(512)  NULL,
    output_size_bytes    BIGINT        NULL,
    rendered_seconds     DOUBLE        NULL,
    billed_seconds       BIGINT        NULL,
    progress             INTEGER       NOT NULL,
    attempts             INTEGER       NOT NULL,
    error_message        VARCHAR(1000) NULL,
    started_at           DATETIME(6)   NULL,
    finished_at          DATETIME(6)   NULL,
    cancel_requested     BIT(1)        NOT NULL
);
CREATE INDEX ix_render_project ON render_jobs (project_id, created_at);
CREATE INDEX ix_render_status ON render_jobs (status, created_at);
