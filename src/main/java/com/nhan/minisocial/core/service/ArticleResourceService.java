package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.resource.ArticleResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleResourceService {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    private Article createOrUpdate(Article article) {
        return articleService.save(article);
    }

    public Article toEntity(ArticleResource articleResource, UserPrincipal userPrincipal){
        Article article = new Article();
        User user = userService.getUser(userPrincipal.getId());
        return null;
    }
}
