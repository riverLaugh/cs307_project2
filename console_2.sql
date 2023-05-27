create table authors
(
    author_id                char(18) primary key,
    author_registration_time timestamp,
    author_phone             char(11),
    author_name              varchar unique,
    password                 varchar,
    unique (author_id),
    unique (author_name)
);


create table posts
(
    ID           serial primary key,
    title        varchar,
    content      varchar,
    posting_time timestamp,
    posting_city varchar,
    author_name  varchar,
    foreign key (author_name) references authors (author_name)
);

create table second_replies
(
    id          serial primary key,
    stars       int,
    author_name varchar,
    content     varchar,
    foreign key (author_name) references authors (author_name)
);

create table replies
(
    reply_id    serial primary key,
    postID      int,
    content     varchar,
    stars       int,
    author_name varchar,
    foreign key (author_name) references authors (author_name)
);

create table replies_to_second_replies
(
    reply_id        int,
    second_reply_id int,
    foreign key (reply_id) references replies (reply_id),
    foreign key (second_reply_id) references second_replies (id),
    unique (reply_id, second_reply_id)
);


create table author_followed
(
    id                   serial primary key,
    author_name          varchar,
    followed_author_name varchar,
    foreign key (author_name) references authors (author_name),
    foreign key (followed_author_name) references authors (author_name),
    unique (author_name, followed_author_name)
);

create table author_favorited
(
    id                    serial primary key,
    post_id               int,
    favorited_author_name varchar,
    foreign key (post_id) references posts (ID),
    foreign key (favorited_author_name) references authors (author_name),
    unique (post_id, favorited_author_name)
);

create table share_author
(
    id                 serial primary key,
    post_id            int,
    shared_author_name varchar,
    foreign key (post_id) references posts (ID),
    foreign key (shared_author_name) references authors (author_name),
    unique (post_id, shared_author_name)
);

create table like_post
(
    id          serial primary key,
    post_id     int,
    author_name varchar,
    foreign key (post_id) references posts (ID),
    foreign key (author_name) references authors (author_name),
    unique (post_id, author_name)
);

create table category_post
(
    id       serial primary key,
    post_id  int,
    category varchar,
    foreign key (post_id) references posts (ID),
    unique (post_id, category)
);
create table blocklist
(
    id           serial,
    author_name  varchar,
    blocked_name varchar,
    unique (author_name, blocked_name)
);
create table search_record
(
    id          serial,
    author_name varchar,
    post_id     int,
    unique(author_name,post_id),
    foreign key (post_id) references posts (ID),
    foreign key (author_name) references authors (author_name)
);
