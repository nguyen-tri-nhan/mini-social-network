import MContext from "../model/MContext";

const UserSmallAvatar = () => {
  const user = MContext.user;
  return (
    <div className="user-avatar">
      {user && <img src={user.avatar} alt={user.fullname} />}
    </div>
  )
};

export default UserSmallAvatar;