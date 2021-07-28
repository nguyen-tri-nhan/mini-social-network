import MAuth from '../model/MAuth';

const Logout = () => {
  MAuth.logout();
  window.location.href = '/login';
  return (null);
}

export default Logout;