package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import com.nhan.minisocial.core.service.ArticleResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleFacade {

    @Autowired
    private ArticleResourceService articleResourceService;

    public Article createOrUpdate(ArticleResource articleResource, UserPrincipal userPrincipal){
        Article article = articleResourceService.toEntity(articleResource, userPrincipal);
        return articleResourceService.createOrUpdate(article);
    }

    public List<ArticleResource> getAll(){
        return articleResourceService.getAll();
    }
}
