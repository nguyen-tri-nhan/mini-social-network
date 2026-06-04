import { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Image, X } from 'lucide-react'
import { toast } from 'sonner'
import { articlesApi } from '../../api/articles'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { Avatar, Button, Textarea, Card } from '../ui'

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

  const canPost = (text.trim() || file) && !create.isPending

  return (
    <Card className="p-4 mb-4">
      <div className="flex gap-3">
        <Avatar fallback={fallback} src={user?.avatarUrl} />
        <button
          onClick={() => setOpen(true)}
          className="flex-1 rounded-full border border-gray-200 bg-gray-50 px-4 py-2 text-left text-sm text-gray-400 hover:bg-gray-100"
        >
          What's on your mind, {user?.firstname}?
        </button>
      </div>

      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4">
              <h3 className="font-semibold text-gray-900">Create post</h3>
              <button onClick={() => setOpen(false)} className="rounded-full p-1 hover:bg-gray-100">
                <X className="h-5 w-5 text-gray-500" />
              </button>
            </div>

            {/* Body */}
            <div className="p-5">
              <div className="mb-3 flex items-center gap-3">
                <Avatar fallback={fallback} src={user?.avatarUrl} />
                <span className="font-medium text-gray-900">{user?.firstname} {user?.lastname}</span>
              </div>

              <Textarea
                placeholder="What's on your mind?"
                rows={4}
                value={text}
                onChange={(e) => setText(e.target.value)}
                className="mb-3"
                autoFocus
              />

              {preview && (
                <div className="relative mb-3">
                  <img src={preview} alt="" className="max-h-64 w-full rounded-lg object-cover" />
                  <button
                    onClick={() => { setPreview(null); setFile(null) }}
                    className="absolute right-2 top-2 rounded-full bg-black/50 p-1 text-white hover:bg-black/70"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              )}

              <button
                onClick={() => fileRef.current?.click()}
                className="flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50"
              >
                <Image className="h-4 w-4" /> Add photo
              </button>
              <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={pickFile} />
            </div>

            {/* Footer */}
            <div className="border-t border-gray-100 px-5 py-3">
              <Button className="w-full" disabled={!canPost} loading={create.isPending} onClick={() => create.mutate()}>
                Post
              </Button>
            </div>
          </div>
        </div>
      )}
    </Card>
  )
}
