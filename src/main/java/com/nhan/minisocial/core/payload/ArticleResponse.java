package com.nhan.minisocial.core.payload;

import com.nhan.minisocial.core.entity.CommentEntity;

import java.time.Instant;
import java.util.List;

public class ArticleResponse {
    private long id;
    private String description;
    private String image;
    private Instant createDate;
    private UserSummary createdBy;
    private long vote;
    private List<CommentEntity> comments;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public UserSummary getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserSummary createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public long getVote() {
        return vote;
    }

    public void setVote(long vote) {
        this.vote = vote;
    }

    public List<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(List<CommentEntity> comments) {
        this.comments = comments;
    }
}
