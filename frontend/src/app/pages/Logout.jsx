
const Logout = () => {
  console.log(localStorage.getItem("JWT"));
  localStorage.setItem("JWT", undefined);
  console.log(localStorage.getItem("JWT"));
  window.location.href = "/login";
  return (null);
}

export default Logout;