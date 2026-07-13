/// <reference lib="webworker" />
import Parser from 'web-tree-sitter'
// web-tree-sitter 0.20 only exports its JavaScript entry. Use a filesystem-relative
// asset import so Vite can serve the bundled runtime Wasm in both dev and build modes.
import treeSitterCoreUrl from '../../../node_modules/web-tree-sitter/tree-sitter.wasm?url'
import cppGrammarUrl from 'tree-sitter-wasms/out/tree-sitter-cpp.wasm?url'
import { TeachingInterpreter, asRuntimeError } from '../interpreter/TeachingInterpreter'
import { parseTeachingCpp } from '../parser/teachingParser'
import type { ParseError, WorkerRequest, WorkerResponse } from '../types'

let parserReady: Promise<Parser> | undefined

async function getParser() {
  if (!parserReady) {
    parserReady = (async () => {
      await Parser.init({ locateFile: () => treeSitterCoreUrl })
      const parser = new Parser()
      parser.setLanguage(await Parser.Language.load(cppGrammarUrl))
      return parser
    })()
  }
  return parserReady
}

async function validateWithTreeSitter(code: string) {
  const parser = await getParser()
  const tree = parser.parse(code)
  if (!tree) throw new Error('Tree-sitter 没有生成语法树。')
  const errors: ParseError[] = tree.rootNode.descendantsOfType(['ERROR', 'MISSING']).map((node) => ({
    line: node.startPosition.row + 1,
    column: node.startPosition.column + 1,
    message: '这里的 C++ 语法看起来不完整，请检查括号、分号或关键字。'
  }))
  if (tree.rootNode.hasError() && errors.length === 0) errors.push({ line: 1, column: 1, message: '代码里有一处 C++ 语法无法解析。' })
  const summary = tree.rootNode.toString().slice(0, 1200)
  tree.delete()
  return { errors, summary }
}

function send(message: WorkerResponse) {
  postMessage(message)
}

self.onmessage = async (event: MessageEvent<WorkerRequest>) => {
  const { code } = event.data
  try {
    const syntax = await validateWithTreeSitter(code)
    if (syntax.errors.length) { send({ type: 'parse_error', errors: syntax.errors }); return }
    let program
    try {
      program = parseTeachingCpp(code)
    } catch (error) {
      send({ type: 'parse_error', errors: Array.isArray(error) ? error as ParseError[] : [{ line: 1, column: 1, message: '代码暂时无法转换为可执行的教学语法树。' }] })
      return
    }
    send({ type: 'parse_success', treeSummary: syntax.summary })
    if (event.data.type === 'run') {
      const result = new TeachingInterpreter([...event.data.input]).run(program, syntax.summary)
      send({ type: 'run_complete', result })
    }
  } catch (error) {
    send({ type: 'runtime_error', error: asRuntimeError(error) })
  }
}
