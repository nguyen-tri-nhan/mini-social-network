import { Button } from 'reactstrap'

const Article = (props) => {
  const { article } = props;
  return (
    <div className="article">
      <div className="article-owner d-flex">
        <Button color='link'>{article.user.fullname}</Button>
      </div>
      <div className="article-desc d-flex">
        {article.description}
      </div>
      <div className="article-image">
        <img src={article.image} alt="" />
      </div>
    </div>
  );
};

export default Article;