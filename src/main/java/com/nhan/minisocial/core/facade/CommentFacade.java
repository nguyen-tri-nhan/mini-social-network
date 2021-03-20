package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class CommentFacade {
    public void commentAPost(UserPrincipal user, long articleId, CommentRequest comment) {

    }
}
