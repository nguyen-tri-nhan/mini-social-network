package com.nhan.minisocial.core.entity;


import com.nhan.minisocial.core.entity.audit.UserDateAudit;

import javax.persistence.*;

@Entity
@Table(name = "notification")
public class NotificationEntity extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private byte type;

    private boolean seen;

    @ManyToOne
    @JoinColumn(name = "from_user")
    private UserEntity fromUser;

    @ManyToOne
    @JoinColumn(name = "to_user")
    private UserEntity toUser;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private ArticleEntity article;

}
