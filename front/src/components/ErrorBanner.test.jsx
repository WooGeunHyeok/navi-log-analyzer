import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  it('에러 메시지를 보여준다', () => {
    render(<ErrorBanner message="분석에 실패했습니다." onRetry={() => {}} />)
    expect(screen.getByText('분석에 실패했습니다.')).toBeInTheDocument()
  })

  it('버튼을 클릭하면 onRetry가 호출된다', async () => {
    const onRetry = vi.fn()
    const user = userEvent.setup()
    render(<ErrorBanner message="분석에 실패했습니다." onRetry={onRetry} />)

    await user.click(screen.getByRole('button', { name: '처음부터 다시 시도' }))

    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
