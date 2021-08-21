import React from 'react';
import CreateArticleForm from '../component/CreateArticleForm';

const HomePage = (props) => {
  document.title = 'Trang chủ';
  return (
    <>
      <div className="container">
        <CreateArticleForm />
      </div>
    </>
  );
}

export default HomePage;