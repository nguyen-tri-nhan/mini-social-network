// import Button from '@material-ui/core/Button';

// const NavigationBar = ({ user }) => {
//   return (
//     <div className="nav-bar">
//       {user ? (
//         <div>
//           hello {user.fullname}
//         </div>
//       ) : (
//         <div>
//           loading
//         </div>
//       )}
//     </div>
//   )
// }

// export default NavigationBar;

import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  CssBaseline,
  IconButton
} from '@material-ui/core';
import { ExitToApp } from '@material-ui/icons';
import HideOnScroll from './HideOnScroll';
import { Button } from 'reactstrap';

const renderNavbar = (user, ...props) => {
  return (
    <div className='nav-bar'>
      <CssBaseline />
      <HideOnScroll {...props}>
        <AppBar>
          <Toolbar>
            <Typography variant="h6">{user.fullname}</Typography>
            <IconButton color={"inherit"} size={"small"}>
              <ExitToApp />
            </IconButton>
          </Toolbar>
        </AppBar>
      </HideOnScroll>
      <Toolbar />
    </div>
  );
}

const renderLoading = (...props) => {
  return (
    <div className='nav-bar'>
      <CssBaseline />
      <HideOnScroll {...props}>
        <AppBar>
          <Toolbar>
            <Typography variant="h6">...Loading</Typography>
          </Toolbar>
        </AppBar>
      </HideOnScroll>
      <Toolbar />
    </div>
  )
}

const NavigationBar = (props) => {
  const { user } = props;
  return user ? renderNavbar(user, props) : renderLoading(props);
}

export default NavigationBar;