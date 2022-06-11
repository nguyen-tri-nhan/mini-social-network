import Service from '../service/Service';
import Cookies from 'cookies-js';
import { goTo } from '../utils/RouteUtils';

const MAuth = {

  isLoggedIn() {
    return localStorage.getItem("User") ? true : false;
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
        const token = `Bearer ${accessToken}`;
        localStorage.setItem("JWT", token);
        return token;
      })
      .then((token) => {
        Service.getMe(token)
          .then(({ data }) => {
            localStorage.setItem("User", JSON.stringify(data));
          })
          .then(() => {
            const isLoggedIn = MAuth.isLoggedIn();
            if (isLoggedIn) {
              goTo("/");
            }
          });
      })
  },

}

export default MAuth;
