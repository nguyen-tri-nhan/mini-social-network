import React from 'react';
import CreateArticleForm from '../component/CreateArticleForm';
import NewsFeed from '../component/NewsFeed';

const HomePage = (props) => {
  document.title = 'Trang chủ';
  return (
    <>
      <div className="container">
        <CreateArticleForm />
        <NewsFeed />
      </div>
    </>
  );
}

export default HomePage;