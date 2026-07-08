export const problemStatusText: Record<string, string> = {
  UNTRIED: '未尝试',
  TRIED: '已尝试',
  ACCEPTED: '已通过',
  PARTIAL: '部分通过'
}

export const submissionStatusText: Record<string, string> = {
  PENDING: '等待判题',
  JUDGING: '判题中',
  ACCEPTED: '通过',
  WRONG_ANSWER: '答案错误',
  COMPILE_ERROR: '编译错误',
  RUNTIME_ERROR: '运行错误',
  TIME_LIMIT: '超时',
  MEMORY_LIMIT: '内存超限',
  OUTPUT_LIMIT: '输出超限',
  SYSTEM_ERROR: '系统错误'
}

export function difficultyText(value?: string): string {
  return ({ easy: '入门', medium: '进阶', hard: '挑战' } as Record<string, string>)[value || ''] || value || '-'
}
