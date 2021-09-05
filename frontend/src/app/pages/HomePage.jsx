import React from 'react';
import { Col, Row } from 'reactstrap';
import CreateArticleForm from '../component/CreateArticleForm';
import LeftSideBar from '../component/LeftSideBar';
import NewsFeed from '../component/NewsFeed';

const HomePage = (props) => {
  document.title = 'Trang chủ';
  return (
    <>
      <Row>
        <Col md={3} lg={4}>
          <LeftSideBar />
        </Col>
        <Col md={6} lg={4}>
          <CreateArticleForm />
          <NewsFeed />
        </Col>
        <Col md={3} lg={4}>
        </Col>
      </Row>
    </>
  );
}

export default HomePage;