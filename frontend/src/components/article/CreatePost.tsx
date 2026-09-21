import { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Image, X } from 'lucide-react'
import { toast } from 'sonner'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import MuiAvatar from '@mui/material/Avatar'
import ButtonBase from '@mui/material/ButtonBase'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogActions from '@mui/material/DialogActions'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { articlesApi } from '../../api/articles'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'

export function CreatePost() {
  const { user } = useAuthStore()
  const qc = useQueryClient()
  const [open, setOpen]       = useState(false)
  const [text, setText]       = useState('')
  const [preview, setPreview] = useState<string | null>(null)
  const [file, setFile]       = useState<File | null>(null)
  const fileRef               = useRef<HTMLInputElement>(null)

  const fallback = user ? `${user.firstname[0]}${user.lastname[0]}`.toUpperCase() : '?'

  const create = useMutation({
    mutationFn: async () => {
      let imageUrl: string | undefined

      if (file) {
        const { uploadUrl, imageUrl: url } = await articlesApi.presignUpload(file.name, file.type)
        await fetch(uploadUrl, { method: 'PUT', body: file, headers: { 'Content-Type': file.type } })
        imageUrl = url
      }

      return articlesApi.create({ description: text || undefined, imageUrl })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.articles.all })
      setText('')
      setPreview(null)
      setFile(null)
      setOpen(false)
      toast.success('Post created!')
    },
  })

  function pickFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    if (f.size > 5 * 1024 * 1024) { toast.error('Max file size 5 MB'); return }
    setFile(f)
    setPreview(URL.createObjectURL(f))
  }

  const canPost = Boolean((text.trim() || file) && !create.isPending)

  return (
    <Card sx={{ p: 2, mb: 2 }}>
      <Box sx={{ display: 'flex', gap: 1.5 }}>
        <MuiAvatar src={user?.avatarUrl} sx={{ fontSize: 14 }}>
          {!user?.avatarUrl && fallback}
        </MuiAvatar>
        <ButtonBase
          onClick={() => setOpen(true)}
          sx={{
            flex: 1, borderRadius: 999, border: 1, borderColor: 'grey.200', bgcolor: 'grey.50',
            px: 2, py: 1, justifyContent: 'flex-start', fontSize: 14, color: 'text.disabled',
            '&:hover': { bgcolor: 'grey.100' },
          }}
        >
          What's on your mind, {user?.firstname}?
        </ButtonBase>
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          Create post
          <IconButton aria-label="Close" size="small" onClick={() => setOpen(false)}><X size={20} /></IconButton>
        </DialogTitle>

        <DialogContent>
          <Box sx={{ mb: 1.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <MuiAvatar src={user?.avatarUrl} sx={{ fontSize: 14 }}>
              {!user?.avatarUrl && fallback}
            </MuiAvatar>
            <Typography fontWeight={500}>{user?.firstname} {user?.lastname}</Typography>
          </Box>

          <TextField
            placeholder="What's on your mind?"
            multiline
            minRows={4}
            fullWidth
            value={text}
            onChange={(e) => setText(e.target.value)}
            autoFocus
            sx={{ mb: 1.5 }}
          />

          {preview && (
            <Box sx={{ position: 'relative', mb: 1.5 }}>
              <Box component="img" src={preview} alt="" sx={{ maxHeight: 256, width: '100%', borderRadius: 2, objectFit: 'cover' }} />
              <IconButton
                aria-label="Remove image"
                size="small"
                onClick={() => { setPreview(null); setFile(null) }}
                sx={{ position: 'absolute', right: 8, top: 8, bgcolor: 'rgba(0,0,0,0.5)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,0.7)' } }}
              >
                <X size={16} />
              </IconButton>
            </Box>
          )}

          <Button
            variant="outlined"
            size="small"
            startIcon={<Image size={16} />}
            onClick={() => fileRef.current?.click()}
          >
            Add photo
          </Button>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={pickFile} />
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button
            fullWidth
            variant="contained"
            disabled={!canPost}
            onClick={() => create.mutate()}
          >
            {create.isPending ? 'Posting…' : 'Post'}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  )
}
