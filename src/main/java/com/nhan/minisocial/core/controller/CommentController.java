package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.resource.CommentResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    @PostMapping(Api.ArticleApi.COMMENT)
    @ResponseStatus(HttpStatus.OK)
    public CommentResource commentAnArticle(@CurrentUser UserPrincipal user,
                                            @PathVariable long articleId,
                                            @RequestBody CommentRequest comment) {
        return null;
    }
}
