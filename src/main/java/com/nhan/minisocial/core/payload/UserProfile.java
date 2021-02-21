package com.nhan.minisocial.core.payload;

import com.nhan.minisocial.core.entity.ArticleEntity;

import java.time.Instant;
import java.util.List;

public class UserProfile {
    private long id;
    private String username;
    private String firstname;
    private String lastname;
    private Instant joinedAt;
    private List<ArticleResponse> articles;
}
