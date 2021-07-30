import axios from 'axios';
import Service from '../service/Service';
import Cookies from 'cookies-js';

const MAuth = {

  isLoggedIn() {
    this.setHeader();
    console.log('set header', axios.defaults.headers.common.Authorization);
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

  login(values) {
    console.log('logged in?',this.isLoggedIn());
    let user = {
      usernameOrEmail: values.usernameOrEmail,
      password: values.password
    }
    Service.login(user).then(({ data }) => {
      console.log(data);
      const { accessToken } = data;
      localStorage.setItem("JWT", accessToken);
      Cookies.set("JWT", accessToken);
      this.setHeader();
      this.getMe();
    });
  },

  getMe() {
    Service.getMe().then(({ data }) => {
      console.log(data);
    });
  }

}

export default MAuth;