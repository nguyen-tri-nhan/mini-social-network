import React from 'react';
import { Col, Row } from 'reactstrap';
import CreateArticleForm from '../component/CreateArticleForm';
import NewsFeed from '../component/NewsFeed';

const HomePage = (props) => {
  document.title = 'Trang chủ';
  return (
    <>
      <Row>
        <Col md={3}>
        </Col>
        <Col md={6}>
          <CreateArticleForm />
          <NewsFeed />
        </Col>
        <Col md={3}>
        </Col>
      </Row>
    </>
  );
}

export default HomePage;