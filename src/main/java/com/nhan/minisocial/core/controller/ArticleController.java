package com.nhan.minisocial.core.controller;


import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.facade.ArticleFacade;
import com.nhan.minisocial.core.payload.ApiResponse;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
public class ArticleController {

    @Autowired
    private ArticleFacade articleFacade;

    @PostMapping(Api.ArticleApi.POST)
    public ResponseEntity<?> postArticle(@CurrentUser UserPrincipal user, @Valid @RequestBody ArticleResource articleResource){
        Article article = articleFacade.createOrUpdate(articleResource, user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path(Api.ArticleApi.GET)
                .buildAndExpand(article.getId()).toUri();
        return ResponseEntity.created(location).body(new ApiResponse(true, "Article saved successfully"));
    }

//    @GetMapping(Api.ArticleApi.COLLECTION)
//    public List<ArticleResource> loadArticle(){
//
//    }
}
