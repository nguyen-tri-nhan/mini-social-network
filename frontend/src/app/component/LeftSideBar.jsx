import { List, ListItem, ListItemIcon } from '@material-ui/core';
import { Group } from '@material-ui/icons';
import MContext from '../model/MContext';
import ArticleOwner from './ArticleOwner';
import StickyBox from 'react-sticky-box';
const LeftSideBar = (props) => {

  const user = MContext.user;

  return (
    <StickyBox offsetTop={42} offsetBottom={20}>
      <div className="left-side-bar">
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
    </StickyBox>
  )
};

export default LeftSideBar;