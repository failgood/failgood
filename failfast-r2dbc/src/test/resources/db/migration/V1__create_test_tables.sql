create sequence users_id_seq no maxvalue;
create table users
(
    id   bigint       not null default nextval('users_id_seq') primary key,
    name varchar(100) not null
);
