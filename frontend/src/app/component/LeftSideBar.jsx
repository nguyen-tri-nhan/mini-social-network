import { List, ListItem, ListItemIcon } from '@material-ui/core';
import { Public, Group } from '@material-ui/icons';
import MContext from '../model/MContext';
import ArticleOwner from './ArticleOwner';
const LeftSideBar = (props) => {

  const user = MContext.user;
  return (
    <div>
      <List component="nav">
        <ListItem button>
          <ArticleOwner user={user} />
        </ListItem>
        <ListItem button>
          <ListItemIcon>
            <Group />
            Bạn bè
          </ListItemIcon>
        </ListItem>
      </List>
    </div>
  )
};

export default LeftSideBar;