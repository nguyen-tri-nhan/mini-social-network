package com.nhan.minisocial.core.entity;

public class ArticleVoteCount {
    private long articleId;

    private long voteCount;

    public ArticleVoteCount(long articleId, long voteCount) {
        this.articleId = articleId;
        this.voteCount = voteCount;
    }

    public long getArticleId() {
        return articleId;
    }

    public void setArticleId(long articleId) {
        this.articleId = articleId;
    }

    public long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(long voteCount) {
        this.voteCount = voteCount;
    }
}
