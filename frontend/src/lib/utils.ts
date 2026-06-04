import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { formatDistanceToNow } from 'date-fns'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function relativeTime(date: string) {
  return formatDistanceToNow(new Date(date), { addSuffix: true })
}

export function initials(firstname: string, lastname: string) {
  return `${firstname[0] ?? ''}${lastname[0] ?? ''}`.toUpperCase()
}
