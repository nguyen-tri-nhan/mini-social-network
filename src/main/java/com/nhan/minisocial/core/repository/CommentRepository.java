package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentRepository> findAllByArticle(long articleId);
}
