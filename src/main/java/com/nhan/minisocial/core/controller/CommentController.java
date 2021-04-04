package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.facade.CommentFacade;
import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.resource.CommentResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {

    @Autowired
    private CommentFacade commentFacade;

    @PostMapping(Api.ArticleApi.COMMENT)
    @ResponseStatus(HttpStatus.OK)
    public void commentAnArticle(@CurrentUser UserPrincipal user,
                                 @PathVariable long id,//articleId
                                 @RequestBody CommentRequest comment) {
        commentFacade.commentAnArticle(user, id, comment);
    }

    @GetMapping(Api.ArticleApi.COMMENT)
    public List<CommentResource> listComments(@CurrentUser UserPrincipal user,
                                              @PathVariable long id) {
        return commentFacade.getComments(user, id);
    }
}
