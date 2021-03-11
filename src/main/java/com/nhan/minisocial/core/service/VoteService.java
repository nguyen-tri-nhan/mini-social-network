package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoteService {

    @Autowired
    private VoteRepository voteRepository;

    public long countVoteByArticleId(long id){
        return voteRepository.countVotesByArticleId(id);
    }
}
