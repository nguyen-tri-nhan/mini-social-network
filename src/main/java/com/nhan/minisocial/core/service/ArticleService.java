package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Article;
import com.nhan.minisocial.core.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;


    public List<Article> getAll() {
        return articleRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Article getOne(long id) {
        return articleRepository.getOne(id);
    }

    public List<Article> getArticleByUser(long id) {
        return articleRepository.findAllByUser(id);
    }

    public void unstatisticsActicle(Long articleId) {
        Article article = getOne(articleId);
        article.setStatistics(false);
        unstatisticsArticle(article);
    }

    public void unstatisticsArticle(Article article) {
        article.setStatistics(false);
        save(article);
    }

    public Article save(Article entity) {
        return articleRepository.save(entity);
    }

}
