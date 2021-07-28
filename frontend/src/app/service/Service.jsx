import http from './http';

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const LOGIN = `${AUTH}/signin`;
const SIGNUP = `${AUTH}/signup`;
const USER = `${API_V1}/user`;
const GET_ME = `${USER}/getme`;
const Service = {


    signup(user) {
        return http.post(SIGNUP, user);
    },

    login(user) {
        return http.post(LOGIN, user);
    },

    getMe() {
        return http.get(GET_ME);
    },
}

export default Service;