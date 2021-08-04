import Button from '@material-ui/core/Button';

const NavigationBar = ({ user }) => {
  return (
  <div className="nav-bar">
    {user && (
      <div>
        hello {user.fullname}  
      </div>
    )}
  </div>
  )
}

export default NavigationBar;