package com.nhan.minisocial.core.facade;

import com.nhan.minisocial.core.entity.Vote;
import com.nhan.minisocial.core.payload.VoteRequest;
import com.nhan.minisocial.core.service.VoteResourceService;
import com.nhan.minisocial.core.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoteFacade {

    @Autowired
    private VoteResourceService voteResourceService;

    public void voteArticle(VoteRequest voteRequest){
        voteResourceService.voteArticle(voteRequest);
    }
}
