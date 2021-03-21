package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.entity.Comment;
import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.resource.CommentResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentResourceService {

    @Autowired
    private UserResourceService userResourceService;

    @Autowired
    private ArticleService articleService;

    private Comment toEntity(CommentRequest commentRequest) {
        Comment comment = new Comment();
        Article article = articleService.getOne(commentRequest.getArticleId());
        comment.setId(commentRequest.getId());
        comment.setArticle(article);
        comment.setDescription(commentRequest.getDescription());
        return comment;
    }

    private CommentResource toResource(Comment comment){
        CommentResource resource = new CommentResource();
        resource.setId((comment.getId()));
        resource.setArticleId(comment.getArticle().getId());
        resource.setDescription(comment.getDescription());
        resource.setUser(userResourceService.getUser(comment.getUser().getId()));
        return resource;
    }

    public void commentAnArticle(UserPrincipal user, long articleId, CommentRequest commentRequest) {

    }
}
