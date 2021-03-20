package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.security.UserPrincipal;
import com.nhan.minisocial.core.service.CommentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentFacade {

    @Autowired
    private CommentResourceService commentResourceService;

    public void commentAPost(UserPrincipal user, long articleId, CommentRequest comment) {

    }
}
