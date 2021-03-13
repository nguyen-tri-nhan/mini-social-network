package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.entity.Vote;
import com.nhan.minisocial.core.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoteFacade {

    @Autowired
    private VoteService voteService;

    public Vote saveVote(Vote vote) {
        return voteService.save(vote);
    }
}
