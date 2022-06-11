import axios from 'axios';
import MAuth from '../model/MAuth';

const http = {

  get({ url, data, params, errorHandler, headers, authentication }) {
    return this.send('get', url, data, params, errorHandler, headers, authentication);
  },

  post({ url, data, params, errorHandler, headers, authentication }) {
    return this.send('post', url, data, params, errorHandler, headers, authentication);
  },

  put({ url, data, params, errorHandler, headers, authentication }) {
    return this.send('put', url, data, params, errorHandler, headers, authentication);
  },

  delete({ url, data, params, errorHandler, headers, authentication }) {
    return this.send('delete', url, data, params, errorHandler, headers, authentication);
  },

  //try to catch error in this param
  send(method, url, data, params, errorHandler, headers, authentication) {
    let config = {
      headers: {
        'Accept': 'application/json',
        'Authorization': authentication || localStorage.getItem("JWT"),
        ...headers
      },
      method: method,
      url: url,
      data: data,
      params: params,
    };
    return new Promise((resolve, reject) => {
      axios(config)
        .then((response) => resolve(response))
        .catch(({ response, message, request }) => {
          if (response?.status === 401) {
            MAuth.logout();
          }
          if (errorHandler) {
            errorHandler();
          } else {
            console.log(message);
          }
        });
    })
  }
}

export default http;