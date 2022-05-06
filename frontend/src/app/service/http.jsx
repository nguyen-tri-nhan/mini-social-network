import axios from 'axios';
import MAuth from '../model/MAuth';

const http = {

  get({ url, data, params, errorHandler, headers }) {
    return this.send('get', url, data, params, errorHandler);
  },

  post({ url, data, params, errorHandler, headers }) {
    return this.send('post', url, data, params, errorHandler);
  },

  put({ url, data, params, errorHandler, headers }) {
    return this.send('put', url, data, params, errorHandler);
  },

  delete({ url, data, params, errorHandler, headers }) {
    return this.send('delete', url, data, params, errorHandler);
  },

  //try to catch error in this param
  send(method, url, data, params, errorHandler, headers) {
    let config = {
      headers: {
        'Accept': 'application/json',
        'Authorization': localStorage.getItem("JWT"),
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