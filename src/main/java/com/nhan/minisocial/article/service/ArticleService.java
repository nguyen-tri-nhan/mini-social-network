package com.nhan.minisocial.article.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;


    public List<Article> getAll() {
        return articleRepository.findAll();
    }

    public Article getOne(long id) {
        return articleRepository.getOne(id);
    }

    public List<Article> getArticleByUser(long id) {
        return articleRepository.findAllByUserEntity(id);
    }

    public Article save(Article entity) {
        return articleRepository.save(entity);
    }

}
