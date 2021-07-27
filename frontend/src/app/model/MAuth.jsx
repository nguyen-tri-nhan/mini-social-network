import axios from "axios";
import Service from "../service/Service";

class MAuth {

  isLoggedIn() {
    return localStorage.getItem("JWT") !== 'undefined';
  }

  setHeader() {
    const token = localStorage.getItem("JWT");
    if (token === null) {
      axios.defaults.headers.common.Authorization = null;
    } else {
      axios.defaults.headers.common.Authorization = `Bearer ${token}`;
    }
  }

  login(values) {
    console.log(localStorage.getItem("JWT"));
    let user = {
      usernameOrEmail: values.usernameOrEmail,
      password: values.password
    }
    Service.login(user).then(({ data }) => {
      console.log(data);
      const { accessToken } = data;
      localStorage.setItem("JWT", accessToken);
      this.setHeader();
      this.getMe();
    });
  }

  getMe() {
    Service.getMe().then(({ data }) => {
      console.log(data);
    })
  }

}

export default new MAuth();