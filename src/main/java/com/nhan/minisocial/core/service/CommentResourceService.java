package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Comment;
import com.nhan.minisocial.core.resource.CommentResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentResourceService {

    @Autowired
    private UserResourceService userResourceService;

    private Comment toEntity() {
        // TODO: impl here
        return null;
    }

    private CommentResource toResource(Comment comment){
        CommentResource resource = new CommentResource();
        resource.setId((comment.getId()));
        resource.setArticleId(comment.getArticle().getId());
        resource.setDescription(comment.getDescription());
        resource.setUser(userResourceService.getUser(comment.getUser().getId()));
        return resource;
    }

    public CommentResource commentAnArticle() {

        return null;
    }
}
