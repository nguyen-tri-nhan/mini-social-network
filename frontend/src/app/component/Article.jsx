import ArticleOwner from './ArticleOwner';
import Comments from './Comments';
import { Card, Dialog, DialogContent, Divider, IconButton } from '@material-ui/core';
import { Comment, ThumbUp } from '@material-ui/icons';
import { useState } from 'react';

const Article = ({ article }) => {
  const { comment, user, description, image, votes, createAt } = article;

  const countComment = (comment) => {
    return comment != null ? comment.length : 0;
  };

  const [open, setOpen] = useState(false);

  const onButtonLikeClicked = () => {
    console.log('clicked', article.id);
  }

  const onImageClick = (e) => {
    setOpen(true);
  }

  const onDialogClickAway = (e) => {
    e.preventDefault();
    setOpen(false);
  }
  return (
    <>
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
          <img src={image} alt={description} onClick={onImageClick} />
        </div>
        <div className="article-statistic">
          <div className="article-statistic-vote d-flex">
            <ThumbUp color="primary" /> {votes}
          </div>
          <div className="article-statistic-comment d-flex">
            <Comment color="primary" /> {countComment(comment)}
          </div>
        </div>
        <Divider />
        <div className="article-footer">
          <IconButton className="article-like-button" onClick={onButtonLikeClicked}>
            <ThumbUp /> Thích
          </IconButton>
          <IconButton className="article-comment-button" onClick={onButtonLikeClicked}>
            <Comment /> Bình luận
          </IconButton>
        </div>
      </Card>
      <Dialog open={open} onClose={onDialogClickAway} fullWidth maxWidth="md">
        <DialogContent>
          <Card className="dialog-article">
            <div className="article-left-dialog">
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
                <img src={image} alt={description} />
              </div>
            </div>
            <div className="article-right-dialog">
              <Comments />
            </div>
          </Card>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default Article;
