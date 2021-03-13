package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.facade.VoteFacade;
import com.nhan.minisocial.core.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoteController {

    @Autowired
    private VoteFacade voteFacade;

    @PostMapping(Api.VoteApi.VOTE_ARTICLE)
    public void voteArticle(@CurrentUser User user, @PathVariable(name = "postId") long postId){
    }

    @PostMapping(Api.VoteApi.VOTE_COMMENT)
    public void voteComment(@CurrentUser User user, @PathVariable(name = "commentId") long commentId){

    }
}
