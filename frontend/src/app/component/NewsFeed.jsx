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
    articles.map((article, index) => {
      console.log(article);
      return (
        <div>
          {article.description}
        </div>
      )
    });
  }

  const renderArticle = (article) => {
    return (
      <div>
        {article.description}
      </div>
    );
  };

  return (
    <div className="new-feed">
      <h1>this is new feed</h1>
      {isReady && renderArticles(articles)}
    </div>
  )
};

export default NewsFeed;