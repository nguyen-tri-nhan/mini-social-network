
const Article = (props) => {
  const {article} = props;
  return (
    <div>
      <h1>
        {article.description}
      </h1>
    </div>
  );
};

export default Article;