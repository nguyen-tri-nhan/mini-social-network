import axios from 'axios';

const apiKey = process.env.REACT_APP_IMGUR_CLIENTID;
const IMGUR_UPLOAD = 'https://api.imgur.com/3/image';

const ImgurHelper = {
  uploadImage(imageFile) {
    let image;
    toBase64(imageFile).then((result) => {
      image = result;
    })
    const config = {
      headers: {
        'Authorization': `Client-ID ${apiKey}`,
        'Accept': 'application/json'
      },
      method: 'post',
      url: IMGUR_UPLOAD,
      data: {
        image,
        type: 'base64'
      },
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

const toBase64 = file => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = () => resolve(reader.result);
  reader.onerror = error => reject(error);
});

export default ImgurHelper;
