import { Typography } from "@material-ui/core";
import { useState } from "react";
import { useEffect } from "react";
import Service from "../service/Service";
import Article from "./Article";

const NewsFeed = (props) => {

  const [articles, setArticles] = useState();
  const [isReady, setReady] = useState(false);

  useEffect(() => {
    Service.getArticle()
      .then((data) => {
        setArticles(data?.data);
      })
      .then(() => {
        setReady(true);
      })
  }, []);

  const renderArticles = (articles) => {
    return articles.map((article) => {
      return renderArticle(article);
    });
  }

  const renderArticle = (article) => {
    return (
      <Article article={article} />
    );
  };

  return (
    <div className="new-feed">
      <Typography>Bảng tin</Typography>
      {isReady && renderArticles(articles)}
    </div>
  )
};

export default NewsFeed;