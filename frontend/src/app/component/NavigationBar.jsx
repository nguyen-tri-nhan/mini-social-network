import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  CssBaseline,
  IconButton
} from '@material-ui/core';
import HideOnScroll from './HideOnScroll';
import RightNavbarMenu from './RightNavbarMenu';
import Notification from './Notification';


const renderNavbar = (user, ...props) => {
  return (
    <div className='nav-bar'>
      <HideOnScroll {...props}>
        <AppBar>
          <Toolbar className="tool-bar">
            <Typography variant="h6" className="logo">Fake book</Typography>
            <Typography variant="h6" className="search-bar">Search bar</Typography>
            <Notification />
            <RightNavbarMenu user={user} className="right-menu"/>
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