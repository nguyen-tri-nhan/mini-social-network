package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.payload.CommentRequest;
import com.nhan.minisocial.core.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommentController {

    @PostMapping(Api.ArticleApi.COMMENT)
    public ResponseEntity<?> commentAnArticle(@CurrentUser User user,
                                              @PathVariable long articleId,
                                              @RequestBody CommentRequest comment) {
        return null;
    }
}
