package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.entity.Comment;
import com.nhan.minisocial.core.entity.User;
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

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    private Comment toEntity(CommentRequest commentRequest) {
        Comment comment = new Comment();
        comment.setId(commentRequest.getId());
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

    public void commentAnArticle(UserPrincipal currentUser, long articleId, CommentRequest commentRequest) {
        Comment comment = toEntity(commentRequest);
        User user = userService.getUser(currentUser.getId());
        comment.setUser(user);
        commentService.save(comment);
    }
}
