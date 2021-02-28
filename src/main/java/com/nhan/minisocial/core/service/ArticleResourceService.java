package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleResourceService {

    @Autowired
    private ArticleService articleService;

    private Article createOrUpdate(Article article) {
        return articleService.save(article);
    }
}
