import { useState, type KeyboardEvent } from 'react'
import { SendHorizontal } from 'lucide-react'
import { z } from 'zod'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import TextField from '@mui/material/TextField'
import { MAX_MESSAGE_LENGTH } from '../../lib/chat'

const schema = z.string().trim().min(1).max(MAX_MESSAGE_LENGTH)

export function MessageInput({ onSend }: { onSend: (content: string) => void }) {
  const [value, setValue] = useState('')
  const parsed = schema.safeParse(value)
  const tooLong = value.length > MAX_MESSAGE_LENGTH

  function submit() {
    if (!parsed.success) return
    onSend(parsed.data)
    setValue('')
  }

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 1, p: 1.5, borderTop: 1, borderColor: 'divider' }}>
      <TextField
        fullWidth
        multiline
        maxRows={5}
        size="small"
        placeholder="Aa"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        error={tooLong}
        helperText={value.length > MAX_MESSAGE_LENGTH - 500 ? `${value.length}/${MAX_MESSAGE_LENGTH}` : undefined}
        slotProps={{ htmlInput: { 'aria-label': 'Message' } }}
      />
      <IconButton aria-label="Send" color="primary" onClick={submit} disabled={!parsed.success}>
        <SendHorizontal size={20} />
      </IconButton>
    </Box>
  )
}
