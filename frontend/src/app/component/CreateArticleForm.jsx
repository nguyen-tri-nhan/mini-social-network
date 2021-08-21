import { useState } from "react";
import {
  TextField,
  TextareaAutosize,
} from '@material-ui/core';
const CreateArticleForm = (props) => {

  const [outerInput, setOuterInput] = useState();

  return (
    <div className="create-article-form">
      <TextField 
      label="What are you doing? Bro"
      onClick="" 
      value={outerInput} 
      fullWidth/>
    </div>
  )
}

export default CreateArticleForm;