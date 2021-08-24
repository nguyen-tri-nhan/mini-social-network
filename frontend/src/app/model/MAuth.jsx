import Service from '../service/Service';
import Cookies from 'cookies-js';
import { goTo } from '../utils/RouteUtils';

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

  login(values, errorHandler) {
    let user = {
      usernameOrEmail: values.usernameOrEmail,
      password: values.password
    }
    Service.login(user, errorHandler)
      .then(({ data }) => {
        const { accessToken } = data;
        localStorage.setItem("JWT", `Bearer ${accessToken}`);
        Cookies.set("JWT", `Bearer ${accessToken}`);
        const isLoggedIn = MAuth.isLoggedIn();
        if (isLoggedIn) {
          goTo("/");
        }
      });
  },

  getMe() {
    return Service.getMe();
  }

}

export default MAuth;