import React from 'react';
import { Col, Row } from 'reactstrap';
import CreateArticleForm from '../component/CreateArticleForm';
import LeftSideBar from '../component/LeftSideBar';
import NewsFeed from '../component/NewsFeed';
import { useState, useEffect } from "react";
import Service from '../service/Service';

const HomePage = (props) => {
  document.title = 'Trang chủ';

  const [articles, setArticles] = useState();
  const [isReady, setReady] = useState(false);

  const refreshData = () => {
    Service.getArticle()
      .then((data) => {
        setArticles(data?.data);
      })
      .then(() => {
        setReady(true);
      });
  };

  useEffect(() => {
    refreshData();
  }, []);

  const handleAfterCreatePost = () => {
    setReady(false);
    refreshData();
  }

  return (
    <>
      <Row>
        <Col md={3} lg={2}>
          <LeftSideBar />
        </Col>
        <Col md={0} lg={2}>
        </Col>
        <Col md={6} lg={4}>
          <CreateArticleForm handleAfterCreatePost={handleAfterCreatePost} />
          {isReady && <NewsFeed articles={articles} />}
        </Col>
        <Col md={0} lg={2}>
        </Col>
        <Col md={3} lg={2}>
        </Col>
      </Row>
    </>
  );
}

export default HomePage;