import axios from "axios";
import { method } from "lodash";

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const LOGIN = `${API_URL}/login`;
const SIGNUP = `${AUTH}/signup`;
class Service {


    signup(user) {
        return axios({
            method: 'post',
            url: SIGNUP,
            data: user,
        }
        )
    }

    login(username, password) {
        return axios({
            method: 'GET',
            url: LOGIN,
            data: {username, password},
        })
    }
}

export default new Service()