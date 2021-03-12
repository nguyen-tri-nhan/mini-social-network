package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoteResourceService {

    @Autowired
    private VoteService voteService;

    private Vote create(Vote vote) {
        return voteService.save(vote);
    }

    private Vote update(long id, byte type) {
        return voteService.update(id, type);
    }

    public Vote createOrUpdate(Vote newVote) {
        Vote vote = voteService.getVote(newVote.getId());
        if(vote != null){
            return update(vote.getId(), vote.getVote());
        }
        return create(newVote);
    }
}
