import React from 'react';
import CreateArticleForm from '../component/CreateArticleForm';
import ImgurHelper from '../helper/ImgurHelper';

const HomePage = (props) => {
  document.title = 'Trang chủ';
  ImgurHelper.uploadImage();
  return (
    <>
      <div className="container">
        <CreateArticleForm />
      </div>
    </>
  );
}

export default HomePage;