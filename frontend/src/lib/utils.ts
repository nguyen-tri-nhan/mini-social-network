import { formatDistanceToNow } from 'date-fns'

export function relativeTime(date: string) {
  return formatDistanceToNow(new Date(date), { addSuffix: true })
}

export function initials(firstname: string, lastname: string) {
  return `${firstname[0] ?? ''}${lastname[0] ?? ''}`.toUpperCase()
}
