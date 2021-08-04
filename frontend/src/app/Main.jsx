import logo from '../../src/logo.svg';
import Login from './pages/Login';
import SignUp from './pages/SignUp';
import Logout from './pages/Logout';
import HomePage from './pages/HomePage'
import {
  BrowserRouter as Router,
  Switch,
  Route,
} from "react-router-dom";
import AuthenticatedRoute from './routes/AuthenticatedRoute';
import MAuth from './model/MAuth';
import UnauthenticatedRoute from './routes/UnauthenticatedRoute';
import { useState, useEffect } from 'react';
import NavigationBar from './component/NavigationBar';


const Main = () => {
  const [user, setUser] = useState();
  /**
   * this useEffect() is call only 1 like component did mount
   * tend to use in set user and others.
   */
  useEffect(() => {
    MAuth.getMe().then(({data}) => {
      setUser(data);
    });
  }, []);
  console.log('user', user);

  return (
    <div className="Main">
      <div className="container">
        <NavigationBar user={user} />
        <Router>
          <Switch>
            <UnauthenticatedRoute path="/login" exact component={Login} />
            <UnauthenticatedRoute path="/signup" exact component={SignUp} />
            <Route path="/logout" exact component={Logout} />
            <AuthenticatedRoute path="/" exact component={HomePage} />
          </Switch>
        </Router>
      </div>
    </div>
  );
}

export default Main;
