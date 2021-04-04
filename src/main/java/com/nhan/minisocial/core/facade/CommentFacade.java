package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.resource.CommentResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import com.nhan.minisocial.core.service.CommentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentFacade {

    @Autowired
    private CommentResourceService commentResourceService;

    public void commentAnArticle(UserPrincipal user, long articleId, CommentRequest comment) {
        commentResourceService.commentAnArticle(user, articleId, comment);
    }
    public List<CommentResource> getComments(UserPrincipal user, long articleId) {
        return commentResourceService.getComments(articleId);
    }
}
