import MAuth from '../model/MAuth';

/**
 * Null page to destroy auth in context and cookie.
 * @returns route to login page
 */
const Logout = () => {
  MAuth.logout();
  window.location.href = '/login';
  return (null);
}

export default Logout;