import http from './http';

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const ARTICLES = `${API_V1}/articles`
const LOGIN = `${AUTH}/signin`;
const SIGNUP = `${AUTH}/signup`;
const USER = `${API_V1}/user`;
const GET_ME = `${USER}/getme`;
const POST_ARTICLE = `${ARTICLES}/POST`;

const Service = {


    signup(user) {
        return http.post(SIGNUP, user);
    },

    login(user, errorHandler) {
        return http.post(LOGIN, user, null, errorHandler);
    },

    getMe() {
        return http.get(GET_ME);
    },

    createArticle(article) {
        return http.post(POST_ARTICLE, article);
    },

    getArticle() {
        return http.get(ARTICLES);
    }
}

export default Service;