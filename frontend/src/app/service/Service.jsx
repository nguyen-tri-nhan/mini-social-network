import axios from "axios";

const API_URL = "http://localhost:8080"
const API_V1 = `${API_URL}/api`;
const AUTH = `${API_V1}/auth`;
const LOGIN = `${API_URL}/login`;
const SIGNUP = `${AUTH}/signup`;
class Service {


    signup(user) {
        return axios.post(SIGNUP, user)
    }
}

export default new Service()