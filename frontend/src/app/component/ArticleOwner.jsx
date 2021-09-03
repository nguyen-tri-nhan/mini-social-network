import { Button } from 'reactstrap';
import { isoDateToString } from '../utils/TimeUtils';
import BlueTickIcon from './BlueTickIcon';

const ArticleOwner = ({ user, time }) => {
  return (
    <div className="article-owner d-flex">
      <div className="article-owner-avatar">
        <img src={user.avatar} alt={user.fullname} />
      </div>
      <div className="article-owner-display">
        <div className="article-owner-info">
          <Button color='link' className="article-owner-name">{user.fullname}</Button>
          {user.id === 1 && <BlueTickIcon />}
        </div>
        {time && <div className="article-owner-created-at">{isoDateToString(time)}</div>}
      </div>
    </div>
  )
}

export default ArticleOwner;