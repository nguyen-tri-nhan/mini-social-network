import logo from '../../src/logo.svg';
import Login from "./pages/Login";
import SignUp from './pages/SignUp';
import {
  BrowserRouter as Router,
  Switch,
  Route,
  Link
} from "react-router-dom";

function Main() {
  return (
    <div className="App">
      <div className="container">
        <Router>
          <Switch>
            <Route path="/login" exact component={Login} />
            <Route path="/signup" exact component={SignUp} />
          </Switch>
        </Router>
      </div>
    </div>
  );
}

export default Main;
