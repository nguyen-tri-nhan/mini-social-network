package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.facade.CommentFacade;
import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.resource.CommentResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    @Autowired
    private CommentFacade commentFacade;

    @PostMapping(Api.ArticleApi.COMMENT)
    @ResponseStatus(HttpStatus.OK)
    public void commentAnArticle(@CurrentUser UserPrincipal user,
                                 @PathVariable long articleId,
                                 @RequestBody CommentRequest comment) {
        commentFacade.commentAPost(user, articleId, comment);
    }
}
