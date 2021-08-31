import { Button } from 'reactstrap';
import { isoDateToString } from '../utils/TimeUtils';


const Article = ({ article }) => {
  console.log(article)
  const { comment, user, description, image, votes, createAt } = article;

  const countComment = (comment) => {
    return comment != null ? comment.length : 0;
  };

  return (
    <div className="newfeed-article">
      <div className="article-owner d-flex">
        <div className="article-owner-avatar">
          <img src={user.avatar} alt={user.fullname} />
        </div>
        <div className="article-owner-info">
          <Button color='link' className="article-owner-name">{user.fullname}</Button>
          <div className="article-owner-created-at">{isoDateToString(createAt)}</div>
        </div>
      </div>
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