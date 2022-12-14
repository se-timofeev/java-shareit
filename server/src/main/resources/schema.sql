CREATE TABLE IF NOT EXISTS users (
    id bigint generated by default as identity primary key,
    name varchar(256),
    email varchar(512) unique
);

CREATE TABLE IF NOT EXISTS requests (
    id bigint generated by default as identity primary key ,
    description varchar(512),
    requestor_id bigint references users(id) ,
    created timestamp
);

CREATE TABLE IF NOT EXISTS items (
    id bigint generated by default as identity primary key ,
    name varchar(256),
    description varchar(512),
    user_id bigint references users(id) ,
    available boolean,
    request_id bigint references requests(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id bigint generated by default as identity primary key ,
    start_date timestamp,
    end_date timestamp,
    item_id bigint references items(id),
    booker_id bigint references users(id),
    status int
);

CREATE TABLE IF NOT EXISTS comments (
    id bigint generated by default as identity primary key ,
    text varchar(1000),
    item_id bigint references items(id),
    author_id bigint references users(id),
    created timestamp
);



