import axios from 'axios';

const apiKey = process.env.REACT_APP_SECRET_NAME;

const ImgurHelper = {
  uploadImage(image) {
    console.log(apiKey);
  },
}

export default ImgurHelper;
