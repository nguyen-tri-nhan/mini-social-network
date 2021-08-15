import axios from 'axios';

const http = {

  get(url, data, params, errorHandler) {
    return this.send('get', url, data, params, errorHandler);
  },

  post(url, data, params, errorHandler) {
    return this.send('post', url, data, params, errorHandler);
  },

  put(url, data, params, errorHandler) {
    return this.send('put', url, data, params, errorHandler);
  },

  delete(url, data, params, errorHandler) {
    return this.send('delete', url, data, params, errorHandler);
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