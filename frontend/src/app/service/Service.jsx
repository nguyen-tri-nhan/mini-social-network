import axios from "axios";
import { method } from "lodash";

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const LOGIN = `${AUTH}/signin`;
const SIGNUP = `${AUTH}/signup`;
const USER = `${API_V1}/user`;
const GET_ME = `${USER}/getme`;
class Service {


    signup(user) {
        return axios({
            method: 'post',
            url: SIGNUP,
            data: user,
        }
        )
    }

    login(user) {
        return axios({
            method: 'POST',
            url: LOGIN,
            data: user,
        })
    }

    getMe() {
        return axios({
            method: 'GET',
            url: GET_ME,
        })
    }
}

export default new Service()