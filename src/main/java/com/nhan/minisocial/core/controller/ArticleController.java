package com.nhan.minisocial.core.controller;


import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.facade.ArticleFacade;
import com.nhan.minisocial.core.payload.ApiResponse;
import com.nhan.minisocial.core.payload.ArticleRequest;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

@RestController
public class ArticleController {

    @Autowired
    private ArticleFacade articleFacade;

    @PostMapping(Api.ArticleApi.POST)
    public ResponseEntity<?> postArticle(@CurrentUser UserPrincipal user, @Valid @RequestBody ArticleRequest articleRequest){
        ArticleResource articleResource = articleFacade.createOrUpdate(articleRequest, user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path(Api.ArticleApi.GET)
                .buildAndExpand(articleResource.getId()).toUri();
        return ResponseEntity.created(location).body(new ApiResponse(true, "Article saved successfully"));
    }

    @GetMapping(Api.ArticleApi.COLLECTION)
    public Stream<ArticleResource> loadArticle(){
        return articleFacade.getAll();
    }

    @GetMapping(Api.ArticleApi.GET)
    public ArticleResource getArticle(@PathVariable long id){
        return articleFacade.getArticle(id);
    }
}
