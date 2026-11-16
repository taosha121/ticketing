create table users (
    id            bigint      generated always as identity primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(20)  not null default 'BUYER',
    created_at    timestamptz  not null default now()
);
