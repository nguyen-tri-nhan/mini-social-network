package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.payload.ArticleRequest;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import com.nhan.minisocial.core.service.ArticleResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class ArticleFacade {

    @Autowired
    private ArticleResourceService articleResourceService;

    public ArticleResource createOrUpdate(ArticleRequest articleRequest, UserPrincipal userPrincipal){
        return articleResourceService.createOrUpdate(articleRequest, userPrincipal);
    }

    public Stream<ArticleResource> getAll(){
        return articleResourceService.getAll();
    }

    public ArticleResource getArticle(long id){
        return articleResourceService.getArticleResource(id);
    }
}
