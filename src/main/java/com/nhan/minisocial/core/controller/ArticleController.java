package com.nhan.minisocial.core.controller;


import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class ArticleController {

    @PostMapping(Api.ArticleApi.GET)
    public ResponseEntity<?> postArticle(@CurrentUser UserPrincipal user, @Valid @RequestBody ArticleResource articleResource){
        return null;
    }
}
