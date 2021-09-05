import ArticleOwner from './ArticleOwner';
import { Card, Divider, IconButton } from '@material-ui/core';
import { ThumbUp } from '@material-ui/icons';

const Article = ({ article }) => {
  const { comment, user, description, image, votes, createAt } = article;

  const countComment = (comment) => {
    return comment != null ? comment.length : 0;
  };

  const onButtonLikeClicked = () => {
    console.log('clicked' , article.id);
  }

  return (
    <Card className="newfeed-article">
      <div className="article-header d-flex">
        <ArticleOwner
          user={user}
          time={createAt}
        />
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
      <Divider />
      <div className="article-footer">
        <IconButton onClick={() => onButtonLikeClicked()}>
          <ThumbUp color="primary"/> Thích
        </IconButton>
      </div>
    </Card>
  );
};

export default Article;