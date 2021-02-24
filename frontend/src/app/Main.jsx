import logo from '../../src/logo.svg';
import Login from "./pages/Login";
import SignUp from './pages/popup/SignUp';
import {
  BrowserRouter as Router,
  Switch,
  Route,
  Link
} from "react-router-dom";

function Main() {
  return (
    <div className="App">
      <Router>
        <Switch>
          <Route path="/login" exact component={Login} />
          <Route path="/signup" exact component={SignUp} />
        </Switch>
      </Router>
    </div>
  );
}

export default Main;
