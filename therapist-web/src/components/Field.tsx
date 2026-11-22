import { useState, type InputHTMLAttributes } from 'react'
import { Icon } from './Icon'

type IconName = Parameters<typeof Icon>[0]['name']

interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label: string
  icon: IconName
  value: string
  onValue: (value: string) => void
  invalid?: boolean
  labelClass?: string
}

export function Field({ label, icon, value, onValue, invalid, labelClass, ...rest }: FieldProps) {
  return (
    <label className="field">
      <span className={labelClass}>{label}</span>
      <div className={'input-wrap' + (invalid ? ' invalid' : '')}>
        <Icon name={icon} />
        <input value={value} onChange={(e) => onValue(e.target.value)} {...rest} />
      </div>
    </label>
  )
}

export function PasswordField({ label, value, onValue, invalid, labelClass, ...rest }: Omit<FieldProps, 'icon'>) {
  const [visible, setVisible] = useState(false)
  return (
    <label className="field">
      <span className={labelClass}>{label}</span>
      <div className={'input-wrap' + (invalid ? ' invalid' : '')}>
        <Icon name="lock" />
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(e) => onValue(e.target.value)}
          {...rest}
        />
        <button
          type="button"
          className="icon-btn"
          aria-label={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((v) => !v)}
        >
          <Icon name={visible ? 'eyeOff' : 'eye'} />
        </button>
      </div>
    </label>
  )
}
