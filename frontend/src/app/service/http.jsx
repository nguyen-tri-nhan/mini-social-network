import axios from 'axios';

const http = {

  get(url, data, params) {
    return this.send('get', url, data, params);
  },

  post(url, data, params) {
    return this.send('post', url, data, params);
  },

  put(url, data, params) {
    return this.send('put', url, data, params);
  },

  delete(url, data, params) {
    return this.send('delete', url, data, params);
  },

  //try to catch error in this param
  send(method, url, data, params, errorHandler) {
    let config = {
      headers: {
        'Authorization': localStorage.getItem("JWT"),
      },
      method: method,
      url: url,
      data: data,
      params: params,
    };
    return new Promise((resolve, reject) => {
      axios(config)
        .then((response) => resolve(response))
        .catch((error) => {
          if (errorHandler) {
            errorHandler();
          } else {
            console.log(error);
          }
        });
    })
  }
}

export default http;