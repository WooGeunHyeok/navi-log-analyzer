import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import ProcessingStatus from './ProcessingStatus.jsx'

describe('ProcessingStatus', () => {
  it.each([
    ['uploading', '업로드 중...'],
    ['parsing', '파싱 중...'],
    ['analyzing', '분석 중...'],
  ])('subStep=%s 이면 "%s"를 보여준다', (subStep, expectedLabel) => {
    render(<ProcessingStatus subStep={subStep} />)
    expect(screen.getByText(expectedLabel)).toBeInTheDocument()
  })
})
