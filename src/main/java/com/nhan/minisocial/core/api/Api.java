package com.nhan.minisocial.core.api;

public class Api {
    public static final String API = "/api";
    static class Article{
        public static final String COLLECTION = API + "/articles";
        public static final String POST = COLLECTION + "/POST";
        public static final String GET = COLLECTION + "/{id}";
    }
}
