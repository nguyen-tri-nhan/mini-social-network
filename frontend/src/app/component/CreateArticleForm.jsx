import { useState } from 'react';
import {
  Dialog,
  DialogActions,
  DialogContent,
  Button,
  Card,
} from '@material-ui/core';
import { Image, Close } from '@material-ui/icons';
import ImgurHelper from '../helper/ImgurHelper';
import { toBase64 } from '../helper/ImageHelper';
import { Regex } from '../utils/AppConstants';
import Service from '../service/Service';
import { Input } from 'reactstrap';
import UserSmallAvatar from './UserSmallAvatar';

const CreateArticleForm = (props) => {
  const [outerInput, setOuterInput] = useState();
  const [openDialog, setOpenDialog] = useState(false);
  const [imageOutput, setImageOutput] = useState('');
  const [imageBase64, setImageBase64] = useState();

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
      setImageOutput(URL.createObjectURL(img));
      toBase64(img).then((result) => setImageBase64(result.replace(Regex.IMAGE, '')));
    }
  }

  const onImageDeleting = (e) => {
    e.target.files = '';
    setImageOutput('');
    setImageBase64('');
  }

  const onCreateArticleClick = () => {
    const { handleAfterCreatePost } = props;
    ImgurHelper.uploadImage(imageBase64)
      .then(({ data }) => {
        return data?.data.link;
      })
      .then((link) => {
        const article = {
          id: -1,
          description: outerInput,
          image: link
        }
        Service.createArticle(article);
      })
      .then(() => {
        setTimeout(() => {
          handleAfterCreatePost();
        }, 1000);
      })
      .then(() => {
        setOpenDialog(false);
      })
  }

  return (
    <>
      <Card className="create-article-card">
        <div className="create-article-form">
          <UserSmallAvatar
            user={props.user}
          />
          <Input
            className="create-article-trigger"
            placeholder="Bạn đang nghĩ gì"
            onClick={onCreateArticleOpenDialog}
            value={outerInput}
          />
        </div>
      </Card>
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
            {imageOutput &&
              <div className="preview-image-uploading-component">
                <img className="preview-image" alt="temporary" src={imageOutput} />
                <button className="delete-image-button" onClick={onImageDeleting}>
                  <Close />
                </button>
              </div>
            }
            {!imageOutput &&
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
          <Button color="primary" onClick={onCreateArticleClick}>
            Đăng bài
          </Button>
          {
            imageOutput &&
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
