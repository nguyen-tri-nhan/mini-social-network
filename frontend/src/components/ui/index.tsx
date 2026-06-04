import { cn } from '../../lib/utils'
import { type ButtonHTMLAttributes, type InputHTMLAttributes, type TextareaHTMLAttributes, forwardRef } from 'react'

// ── Button ────────────────────────────────────────────────────────────────────

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, children, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed',
        {
          'bg-brand text-white hover:bg-brand-hover':            variant === 'primary',
          'bg-gray-100 text-gray-700 hover:bg-gray-200':         variant === 'secondary',
          'text-gray-600 hover:bg-gray-100':                     variant === 'ghost',
          'bg-red-500 text-white hover:bg-red-600':              variant === 'danger',
          'px-3 py-1.5 text-sm':                                 size === 'sm',
          'px-4 py-2 text-sm':                                   size === 'md',
          'px-5 py-2.5 text-base':                               size === 'lg',
        },
        className,
      )}
      {...props}
    >
      {loading && <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />}
      {children}
    </button>
  ),
)
Button.displayName = 'Button'

// ── Input ─────────────────────────────────────────────────────────────────────

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: string
  label?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, label, id, ...props }, ref) => (
    <div className="flex flex-col gap-1">
      {label && <label htmlFor={id} className="text-sm font-medium text-gray-700">{label}</label>}
      <input
        ref={ref}
        id={id}
        className={cn(
          'w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400',
          'focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand',
          error && 'border-red-500 focus:border-red-500 focus:ring-red-500',
          className,
        )}
        {...props}
      />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  ),
)
Input.displayName = 'Input'

// ── Textarea ──────────────────────────────────────────────────────────────────

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, error, ...props }, ref) => (
    <div className="flex flex-col gap-1">
      <textarea
        ref={ref}
        className={cn(
          'w-full resize-none rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm placeholder-gray-400',
          'focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand',
          error && 'border-red-500',
          className,
        )}
        {...props}
      />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  ),
)
Textarea.displayName = 'Textarea'

// ── Avatar ────────────────────────────────────────────────────────────────────

export function Avatar({ src, fallback, size = 'md' }: { src?: string; fallback: string; size?: 'sm' | 'md' | 'lg' }) {
  const sizeClass = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm', lg: 'h-14 w-14 text-base' }[size]
  return src ? (
    <img src={src} alt="" className={cn('rounded-full object-cover', sizeClass)} />
  ) : (
    <div className={cn('flex items-center justify-center rounded-full bg-brand font-semibold text-white', sizeClass)}>
      {fallback}
    </div>
  )
}

// ── Spinner ───────────────────────────────────────────────────────────────────

export function Spinner({ className }: { className?: string }) {
  return (
    <div className={cn('h-6 w-6 animate-spin rounded-full border-2 border-gray-200 border-t-brand', className)} />
  )
}

// ── Card ──────────────────────────────────────────────────────────────────────

export function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn('rounded-xl bg-white shadow-sm', className)}>{children}</div>
}
