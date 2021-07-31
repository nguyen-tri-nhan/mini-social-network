import axios from 'axios';
import Service from '../service/Service';
import Cookies from 'cookies-js';
const MAuth = {

  isLoggedIn() {
    this.setHeader();
    return axios.defaults.headers.common.Authorization !== 'undefined';
  },

  setHeader() {
    const token = localStorage.getItem("JWT") || Cookies.get("JWT");
    if (token !== 'undefined') {
      axios.defaults.headers.common.Authorization = `Bearer ${token}`;
    } else {
      axios.defaults.headers.common.Authorization = `undefined`;
    }
  },

  logout() {
    localStorage.setItem("JWT", undefined);
    Cookies.set("JWT", undefined);
    this.setHeader();
  },

  async login(values) {
    let user = {
      usernameOrEmail: values.usernameOrEmail,
      password: values.password
    }
    Service.login(user)
      .then(({ data }) => {
        const { accessToken } = data;
        localStorage.setItem("JWT", accessToken);
        Cookies.set("JWT", accessToken);
        this.setHeader();
      });
  },

  getMe() {
    Service.getMe().then(({ data }) => {
      console.log(data);
    });
  }

}

export default MAuth;