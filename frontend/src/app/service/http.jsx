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

  send(method, url, data, params) {
    return axios({
      method: method,
      url: url,
      data: data,
      params: params,
    });
  }
}

export default http;