import type { ExecutionResult, ParseError, RuntimeErrorInfo, WorkerRequest, WorkerResponse } from '../types'

export interface WorkerRunResult {
  result?: ExecutionResult
  parseErrors: ParseError[]
  runtimeError?: RuntimeErrorInfo
}

export function runInExecutionWorker(request: WorkerRequest) {
  return new Promise<WorkerRunResult>((resolve) => {
    const worker = new Worker(new URL('./cppExecution.worker.ts', import.meta.url), { type: 'module' })
    const response: WorkerRunResult = { parseErrors: [] }
    worker.onmessage = (event: MessageEvent<WorkerResponse>) => {
      if (event.data.type === 'parse_error') {
        response.parseErrors = event.data.errors
        worker.terminate()
        resolve(response)
      } else if (event.data.type === 'run_complete') {
        response.result = event.data.result
        worker.terminate()
        resolve(response)
      } else if (event.data.type === 'runtime_error') {
        response.runtimeError = event.data.error
        worker.terminate()
        resolve(response)
      } else if (event.data.type === 'parse_success' && request.type === 'validate') {
        worker.terminate()
        resolve(response)
      }
    }
    worker.onerror = () => {
      response.runtimeError = { line: 1, message: '执行引擎启动失败，请刷新页面后再试。' }
      worker.terminate()
      resolve(response)
    }
    worker.postMessage(request)
  })
}
