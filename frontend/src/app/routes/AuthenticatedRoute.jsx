import React from 'react'
import { Redirect, Route } from 'react-router-dom'
import MAuth from '../model/MAuth';
import { useState, useEffect } from 'react';
import NavigationBar from '../component/NavigationBar';
import MContext from '../model/MContext';

const AuthenticatedRoute = ({ component: Component, ...rest }) => {

  const [user, setUser] = useState();
  /**
   * this useEffect() is call only 1 like component did mount
   * tend to use in set user and others.
   */
  useEffect(() => {
    MAuth.getMe().then(({ data }) => {
      MContext.user = data;
      return data;
    })
    .then((data) => {
      setUser(data);
    });
  }, []);

  // Add your own authentication on the below line.
  const isLoggedIn = MAuth.isLoggedIn();

  return (
    <>
      <NavigationBar user={user} />
      <Route
        {...rest}
        render={props =>
          isLoggedIn ? (
            <Component {...props} />
          ) : (
            <Redirect to={{ pathname: '/login', state: { from: props.location } }} />
          )
        }
      />
    </>
  )
}

export default AuthenticatedRoute