import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import UploadForm from './UploadForm.jsx'

describe('UploadForm', () => {
  it('제목이 비어있으면 에러를 보여주고 onSubmit을 호출하지 않는다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('제목을 입력해주세요.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('파일을 선택하지 않으면 에러를 보여준다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('업로드할 로그 파일을 선택해주세요.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('50MB를 초과하는 파일은 에러를 보여준다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    const bigFile = new File(['content'], 'big.log', { type: 'text/plain' })
    Object.defineProperty(bigFile, 'size', { value: 51 * 1024 * 1024 })
    await user.upload(screen.getByLabelText('로그 파일'), bigFile)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('파일 크기는 50MB를 초과할 수 없습니다.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('유효한 입력이면 onSubmit을 trim된 값으로 호출한다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '  이슈 로그  ')
    await user.type(screen.getByLabelText('Jira 티켓 키 (선택)'), 'NAVI-123')
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(onSubmit).toHaveBeenCalledWith({
      title: '이슈 로그',
      jiraTicketKey: 'NAVI-123',
      file,
    })
  })

  it('jiraTicketKey를 비워두면 undefined로 전달한다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(onSubmit).toHaveBeenCalledWith({ title: '이슈 로그', jiraTicketKey: undefined, file })
  })
})
