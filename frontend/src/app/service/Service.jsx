import axios from "axios";
import { method } from "lodash";

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const LOGIN = `${AUTH}/signin`;
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

    login(user) {
        return axios({
            method: 'POST',
            url: LOGIN,
            data: user,
        })
    }
}

export default new Service()