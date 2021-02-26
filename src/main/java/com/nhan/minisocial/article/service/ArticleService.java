package com.nhan.minisocial.article.service;

import com.nhan.minisocial.core.entity.ArticleEntity;
import com.nhan.minisocial.core.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

//    public ArticleEntity create(ArticleEntity article){
//
//    }


    public List<ArticleEntity> getAll(){
        return articleRepository.findAll();
    }

    public List<ArticleEntity> getArticleByUser(long id){
        return articleRepository.findAllByUserEntity(id);
    }

    public ArticleEntity save(ArticleEntity entity){
        return articleRepository.save(entity);
    }
}
