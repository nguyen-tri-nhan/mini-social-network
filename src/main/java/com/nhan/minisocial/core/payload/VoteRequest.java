package com.nhan.minisocial.core.payload;

public class VoteRequest {

    private long id;
    private byte vote;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte getVote() {
        return vote;
    }

    public void setVote(byte vote) {
        this.vote = vote;
    }
}
