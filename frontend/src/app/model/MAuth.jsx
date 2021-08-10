import axios from 'axios';
import Service from '../service/Service';
import Cookies from 'cookies-js';
const MAuth = {

  isLoggedIn() {
    this.setHeader();
    return localStorage.getItem("JWT") !== 'undefined';
  },

  setHeader() {
    if(Cookies.get("JWT")) {
      localStorage.setItem("JWT", Cookies.get("JWT"));
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
        localStorage.setItem("JWT", `Bearer ${accessToken}`);
        Cookies.set("JWT", `Bearer ${accessToken}`);
        this.setHeader();
      });
  },

  getMe() {
    return Service.getMe();
  }

}

export default MAuth;