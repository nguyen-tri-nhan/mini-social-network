import ArticleOwner from './ArticleOwner';

const Article = ({ article }) => {
  console.log(article)
  const { comment, user, description, image, votes, createAt } = article;

  const countComment = (comment) => {
    return comment != null ? comment.length : 0;
  };

  return (
    <div className="newfeed-article">
      <ArticleOwner
        user={user}
        time={createAt}
      />
      <div className="article-desc d-flex">
        {description}
      </div>
      <div className="article-image">
        <img src={image} alt="" />
      </div>
      <div className="article-statistic">
        <div className="article-statistic-vote">
          {votes} Votes
        </div>
        <div className="article-statistic-comment">
          {countComment(comment)} comments
        </div>
      </div>
    </div>
  );
};

export default Article;