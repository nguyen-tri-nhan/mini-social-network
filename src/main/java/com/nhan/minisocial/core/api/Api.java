package com.nhan.minisocial.core.api;

public class Api {
    public static final String API = "/api";

    public static class ArticleApi {
        public static final String COLLECTION = API + "/articles";
        public static final String POST = COLLECTION + "/POST";
        public static final String GET = COLLECTION + "/{id}";
        public static final String COMMENT = GET + "/comment";
    }

    public static class VoteApi {
        public static final String COLLECTION = API + "/votes";
        public static final String VOTE_ARTICLE = COLLECTION + "/{postId}";
        public static final String VOTE_COMMENT = COLLECTION + "/{commentId}";
    }

    public static class User {
        public static final String USER = API + "/user";
        public static final String GET_ME = USER + "/getme";
    }
}
