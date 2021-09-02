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
import UnauthenticatedRoute from './routes/UnauthenticatedRoute';
import RouteConstants from './routes/RouteConstants';
import { ThemeProvider } from '@material-ui/styles';
import { theme } from './utils/theme';

const Main = () => {

  return (
    <div className="Main">
      <ThemeProvider theme={theme}>
        <Router>
          <Switch>
            <UnauthenticatedRoute path={RouteConstants.login} exact component={Login} />
            <UnauthenticatedRoute path={RouteConstants.signup} exact component={SignUp} />
            <Route path={RouteConstants.logout} exact component={Logout} />
            <AuthenticatedRoute path={RouteConstants.root} exact component={HomePage} />
          </Switch>
        </Router>
      </ThemeProvider>
    </div>
  );
}

export default Main;
