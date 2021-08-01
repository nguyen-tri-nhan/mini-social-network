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
import PrivateRoute from './routes/PrivateRoute';
import PublicRoute from './routes/PublicRoute';

function Main() {
  return (
    <div className="App">
      <div className="container">
        <Router>
          <Switch>
            <PublicRoute path="/login" exact component={Login} />
            <PublicRoute path="/signup" exact component={SignUp} />
            <Route path="/logout" exact component={Logout} />
            <PrivateRoute path="/" exact component={HomePage} />
          </Switch>
        </Router>
      </div>
    </div>
  );
}

export default Main;
