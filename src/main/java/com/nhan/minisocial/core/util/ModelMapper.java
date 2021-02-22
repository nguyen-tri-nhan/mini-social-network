package com.nhan.minisocial.core.util;

import com.nhan.minisocial.core.entity.ArticleEntity;
import com.nhan.minisocial.core.entity.CommentEntity;
import com.nhan.minisocial.core.entity.UserEntity;
import com.nhan.minisocial.core.entity.VoteEntity;
import com.nhan.minisocial.core.payload.ArticleResponse;
import com.nhan.minisocial.core.payload.UserSummary;

import java.util.List;
import java.util.Map;

public class ModelMapper {
    public static ArticleResponse mapArticleToArticleReponse(ArticleEntity article, UserEntity user, List<CommentEntity> comments, long vote){
        ArticleResponse articleResponse = new ArticleResponse();

        articleResponse.setId(article.getId());
        articleResponse.setDescription(article.getDescription());
        articleResponse.setImage(article.getImage());
        articleResponse.setCreatedBy(new UserSummary(
                user.getId(), user.getUsername(),user.getLastname() + " " + user.getFirstname()
        ));
        articleResponse.setCreateDate(article.getCreateAt());
        articleResponse.setComments(comments);
        articleResponse.setVote(vote);
        return articleResponse;
    }
}
