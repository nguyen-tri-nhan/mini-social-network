export const sleep = (ms) => {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export const isoDateToString = (isoDate) => {
  const date = new Date(isoDate);
  return date.getDate()+'-' + (date.getMonth()+1) + '-'+date.getFullYear();
}
