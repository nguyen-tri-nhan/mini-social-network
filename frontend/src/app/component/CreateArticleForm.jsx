import { useState } from "react";
import {
  TextField,
  Dialog,
  DialogActions,
  DialogContent,
  Button,
} from '@material-ui/core';
import { Camera, Image, Close } from '@material-ui/icons';
import ImgurHelper from '../helper/ImgurHelper';
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
    }
  }

  const onImageDeleting = (e) => {
    e.target.files = '';
    setImage('');
    setImageFile('');
  }

  const onUploadImageClick = () => {
    console.log(outerInput);
    console.log(imageFile);
    ImgurHelper.uploadImage(imageFile).then((response) => console.log(response));
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
            {image &&
              <div className="preview-image-uploading-component">
                <img className="preview-image" alt="temporary" src={image} />
                <button className="delete-image-button" onClick={onImageDeleting}>
                  <Close />
                </button>
              </div>
            }
            {!image &&
              <div>
                <label htmlFor="input-image" className="input-img-label"><Image /></label>
                <input
                  id="input-image"
                  style={{ display: 'none' }}
                  type="file"
                  accept="image/*"
                  onChange={onInputImageChange} />
              </div>
            }
          </div>
        </DialogContent>
        <DialogActions>
          <Button color="primary" onClick={onUploadImageClick}>
            Đăng bài
          </Button>
          {
            image &&
            <Button onClick={onImageDeleting}>
              Xoá ảnh
            </Button>
          }
          <Button onClick={onCreateArticleCloseDialog}>
            Huỷ
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

export default CreateArticleForm;