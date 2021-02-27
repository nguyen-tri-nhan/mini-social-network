package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    long countVoteEntitiesByArticle_Id(long articleId);
    Optional<Vote> findByUserId(long Userid);
    List<Vote> findVoteEntityByArticle(long articleId);
}
