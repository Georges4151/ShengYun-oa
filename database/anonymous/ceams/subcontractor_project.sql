create table subcontractor_project
(
    id               int auto_increment
        primary key,
    subcontractor_id int                                not null,
    project_id       int                                not null,
    create_time      datetime default CURRENT_TIMESTAMP null,
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_subcontractor_project
        unique (subcontractor_id, project_id),
    constraint subcontractor_project_ibfk_1
        foreign key (subcontractor_id) references subcontractors (id)
            on delete cascade,
    constraint subcontractor_project_ibfk_2
        foreign key (project_id) references projects (id)
            on delete cascade
);

create index idx_subcontractor_project_project
    on subcontractor_project (project_id);

create index idx_subcontractor_project_subcontractor
    on subcontractor_project (subcontractor_id);

