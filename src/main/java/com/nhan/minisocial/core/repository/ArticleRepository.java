package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.ArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {
    Optional<ArticleEntity> findById(long articleId);

    Page<ArticleEntity> findByCreatedBy(long userId, Pageable pageable);

    long findByIdIn(List<Long> articleId);

    long findByIdIn(List<Long> articleId, Sort sort);

    List<ArticleEntity> findAllByUserEntity(long userId);
}
