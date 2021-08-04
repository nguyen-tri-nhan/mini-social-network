export default {

  insertIf: (condition, ...elements) => (condition ? elements : []),

  intersperse: (arr, sep, decorator = (x) => x) => arr.reduce((prev, curr) => [...prev, sep, decorator(curr)], []).slice(1),
  
};
