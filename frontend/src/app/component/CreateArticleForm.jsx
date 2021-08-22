import { useState } from "react";
import {
  TextField,
  Dialog,
  DialogActions,
  DialogContent,
  Button,
} from '@material-ui/core';
import { Camera, Image } from '@material-ui/icons';
const CreateArticleForm = (props) => {

  const [outerInput, setOuterInput] = useState();
  const [openDialog, setOpenDialog] = useState(false);
  const [image, setImage] = useState('');
  const [imageFile, setImageFile] = useState();

  const onCreateArticleOpenDialog = () => {
    setOpenDialog(true);
  }

  const onCreateArticleCloseDialog = () => {
    setOpenDialog(false);
  }

  const onArticleTextAreaChange = (e) => {
    const value = e.target.value;
    setOuterInput(value);
  }

  const onInputImageChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      let img = e.target.files[0];
      setImage(URL.createObjectURL(img));
      setImageFile(img);
      console.log(img);
    }
  }

  return (
    <>
      <div className="create-article-form">
        <TextField
          label="What are you doing? Bro"
          onClick={onCreateArticleOpenDialog}
          value={outerInput}
          fullWidth />
      </div>
      <Dialog open={openDialog} fullWidth maxWidth="lg">
        <DialogContent>
          <div className="col-xl-12 text-secondary">
            <textarea
              placeholder="Bạn đang nghĩ gì"
              type="text"
              className="form-control"
              value={outerInput}
              rows="7"
              onChange={onArticleTextAreaChange}
            />
            {image && <img alt="temporary" src={image} />}
            <label htmlFor="input-image" className="input-img-label"><Image /></label>
            <input
              id="input-image"
              style={{display:'none'}}
              type="file"
              accept="image/*"
              onChange={onInputImageChange} />
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={onCreateArticleCloseDialog}>
            Huỷ
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

export default CreateArticleForm;