import axios from 'axios';

const apiKey = process.env.REACT_APP_IMGUR_CLIENTID;
const IMGUR_UPLOAD = 'https://api.imgur.com/3/image';

const ImgurHelper = {
  uploadImage(image) {
    const config = {
      headers: {
        'Authorization': `Client-ID ${apiKey}`,
      },
      method: 'post',
      url: IMGUR_UPLOAD,
      data: image,
    };
    return new Promise((resolve, reject) => {
      axios(config)
        .then((response) => resolve(response))
        .catch((error) => {
          console.log(error);
        });
    })
  },
}

export default ImgurHelper;
