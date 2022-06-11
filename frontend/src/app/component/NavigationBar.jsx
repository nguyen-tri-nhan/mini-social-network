import React from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
} from '@material-ui/core';
import {
  Input
} from 'reactstrap';
import HideOnScroll from './HideOnScroll';
import RightNavbarMenu from './RightNavbarMenu';
import Notification from './Notification';
import { goTo } from '../utils/RouteUtils';

const renderNavbar = (user, ...props) => {
  return (
    <div className='nav-bar'>
      <HideOnScroll {...props}>
        <AppBar>
          <Toolbar className="tool-bar">
            <Button onClick={() => goTo("/")} className="logo">
              <Typography variant="h6" className="logo-name">Fakeboob</Typography>
            </Button>
            <div className="nav-bar-seperator-1"/>
            <Input placeholder="Tìm kiếm" className="search-bar"/>
            <div className="nav-bar-seperator-2" />
            <Notification />
            <RightNavbarMenu user={user} className="right-menu" />
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
