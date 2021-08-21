import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  CssBaseline,
  IconButton
} from '@material-ui/core';
import { ExitToApp, Notifications } from '@material-ui/icons';
import HideOnScroll from './HideOnScroll';
import RightNavbarMenu from './RightNavbarMenu';


const renderNavbar = (user, ...props) => {
  return (
    <div className='nav-bar'>
      <HideOnScroll {...props}>
        <AppBar>
          <Toolbar className="tool-bar">
            <Typography variant="h6" className="logo">{user.fullname}</Typography>
            <Typography variant="h6" className="search-bar">Search bar</Typography>
            <Notifications />
            <RightNavbarMenu className="right-menu"/>
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