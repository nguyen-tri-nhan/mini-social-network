package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findById(long articleId);

    Page<Article> findArticlesByCreatedBy(long userId, Pageable pageable);

    List<Article> findArticlesByCreatedBy(long userid);

    long findArticlesByIdIn(List<Long> articleId, Sort sort);

    List<Article> findAllByUser(long id);
}
