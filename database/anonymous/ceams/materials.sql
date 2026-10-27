create table materials
(
    material_id   int auto_increment
        primary key,
    material_name varchar(200)                       not null,
    material_code varchar(50)                        not null,
    specification varchar(200)                       not null,
    unit          varchar(20)                        not null,
    current_price decimal(12, 2)                     not null,
    supplier_id   int                                null,
    category_id   int                                null,
    created_at    datetime default CURRENT_TIMESTAMP not null,
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint material_code
        unique (material_code)
);

