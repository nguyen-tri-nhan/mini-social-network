import http from './http';

const HOST = window.location.hostname;
const API_URL = `http://${HOST}:8080`;
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
        return http.post({url: SIGNUP, data: user});
    },

    login(user, errorHandler) {
        return http.post({url: LOGIN, data: user, errorHandler});
    },

    getMe() {
        return http.get({url: GET_ME});
    },

    createArticle(article) {
        return http.post({url: POST_ARTICLE, data: article});
    },

    getArticle() {
        return http.get({url: ARTICLES});
    }
}

export default Service;