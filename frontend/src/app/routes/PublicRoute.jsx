import React from 'react'
import { Redirect, Route } from 'react-router-dom'
import MAuth from '../model/MAuth';

const PublicRoute = ({ component: Component, ...rest }) => {

  // Add your own authentication on the below line.
  const isLoggedIn = MAuth.isLoggedIn();

  return (
    <Route
      {...rest}
      render={props =>
        !isLoggedIn ? (
          <Component {...props} />
        ) : (
          <Redirect to={{ pathname: '/', state: { from: props.location } }} />
        )
      }
    />
  )
}

export default PublicRoute