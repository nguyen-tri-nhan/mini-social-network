package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.VoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<VoteEntity, Long> {
    long countVoteEntitiesByArticle_Id(long articleId);
    Optional<VoteEntity> findByUserId(long Userid);
    List<VoteEntity> findVoteEntityByArticle(long articleId);
}
