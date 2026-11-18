create table orders (
    id               bigint       generated always as identity primary key,
    user_id          bigint       not null references users(id),
    ticket_type_id   bigint       not null references ticket_types(id),
    quantity         int          not null default 1,
    status           varchar(20)  not null default 'PENDING',
    idempotency_key  varchar(64)  not null unique,
    created_at       timestamptz  not null default now()
);

create table reservations (
    id             bigint      generated always as identity primary key,
    order_id       bigint      not null references orders(id),
    user_id        bigint      not null references users(id),
    ticket_type_id bigint      not null references ticket_types(id),
    quantity       int         not null default 1,
    expires_at     timestamptz not null,
    status         varchar(20) not null default 'ACTIVE',
    created_at     timestamptz not null default now()
);
