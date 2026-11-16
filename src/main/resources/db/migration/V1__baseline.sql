-- 阶段 0 基线迁移：仅用于验证 Flyway 迁移链路是否打通。
-- 阶段 1 开始建真正的业务表（users / events / ...）时，此占位表会被删除。
create table if not exists schema_smoke (
    id         bigint generated always as identity primary key,
    note       varchar(100) not null,
    created_at timestamptz  not null default now()
);

insert into schema_smoke (note) values ('flyway baseline ok');
