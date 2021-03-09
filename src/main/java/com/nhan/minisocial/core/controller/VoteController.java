package com.nhan.minisocial.core.controller;

import com.nhan.minisocial.core.api.Api;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoteController {

    @PostMapping(Api.VoteApi.VOTE_ARTICLE)
    public void voteArticle(@PathVariable(name = "postId") long postId){

    }

    @PostMapping(Api.VoteApi.VOTE_COMMENT)
    public void voteComment(@PathVariable(name = "commentId") long commentId){

    }
}
