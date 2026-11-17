create table events (
    id           bigint       generated always as identity primary key,
    organizer_id bigint       not null references users(id),
    title        varchar(255) not null,
    description  text,
    venue        varchar(255) not null,
    starts_at    timestamptz  not null,
    ends_at      timestamptz  not null,
    status       varchar(20)  not null default 'DRAFT',
    poster_url   varchar(500),
    created_at   timestamptz  not null default now()
);

create table ticket_types (
    id                 bigint         generated always as identity primary key,
    event_id           bigint         not null references events(id),
    name               varchar(100)   not null,
    price              numeric(10, 2) not null,
    total_quantity     int            not null,
    remaining_quantity int            not null,
    created_at         timestamptz    not null default now()
);
