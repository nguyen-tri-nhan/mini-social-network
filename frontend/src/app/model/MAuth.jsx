import Service from '../service/Service';
import Cookies from 'cookies-js';
import { goTo } from '../utils/RouteUtils';
import MContext from './MContext';

const MAuth = {

  isLoggedIn() {
    return localStorage.getItem("JWT") ? true : false;
  },

  logout() {
    localStorage.clear();
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
        this.getMe();
        const isLoggedIn = MAuth.isLoggedIn();
        if (isLoggedIn) {
          goTo("/");
        }
      });
  },

  getMe() {
    return Service.getMe()
      .then(({data}) => {
        localStorage.setItem("User", JSON.stringify(data));
      });
  }

}

export default MAuth;
