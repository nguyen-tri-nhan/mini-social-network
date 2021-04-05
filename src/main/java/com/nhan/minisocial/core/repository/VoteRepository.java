package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findVoteByUserId(long Userid);

    List<Vote> findVotesByArticleId(long articleId);

    long countVoteByArticleIdAndVote(long articleId, byte vote);

    long countVotesByArticleId(long articleId);

}
