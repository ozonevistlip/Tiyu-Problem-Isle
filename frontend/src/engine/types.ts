export type PrimitiveType = 'int' | 'double' | 'bool' | 'char' | 'string' | 'void'

export interface SourceLocation {
  line: number
  column: number
}

export interface ProgramNode {
  kind: 'program'
  functions: FunctionNode[]
}

export interface FunctionNode extends SourceLocation {
  kind: 'function'
  name: string
  returnType: PrimitiveType
  parameters: ParameterNode[]
  body: BlockNode
}

export interface ParameterNode extends SourceLocation {
  name: string
  dataType: PrimitiveType
}

export interface BlockNode extends SourceLocation {
  kind: 'block'
  statements: StatementNode[]
}

export type StatementNode = DeclarationNode | ExpressionStatementNode | IfNode | ForNode | WhileNode | ReturnNode | OutputNode | InputNode | BreakNode | ContinueNode | BlockNode

export interface DeclarationNode extends SourceLocation {
  kind: 'declaration'
  dataType: PrimitiveType
  name: string
  dimensions: ExpressionNode[]
  initializer?: InitializerNode | ExpressionNode
}

export interface InitializerNode extends SourceLocation {
  kind: 'initializer'
  values: Array<InitializerNode | ExpressionNode>
}

export interface ExpressionStatementNode extends SourceLocation {
  kind: 'expression_statement'
  expression: ExpressionNode
}

export interface IfNode extends SourceLocation {
  kind: 'if'
  condition: ExpressionNode
  consequent: BlockNode
  alternate?: BlockNode | IfNode
}

export interface ForNode extends SourceLocation {
  kind: 'for'
  initializer?: DeclarationNode | ExpressionNode
  condition?: ExpressionNode
  update?: ExpressionNode
  body: BlockNode
}

export interface WhileNode extends SourceLocation {
  kind: 'while'
  condition: ExpressionNode
  body: BlockNode
}

export interface ReturnNode extends SourceLocation {
  kind: 'return'
  value?: ExpressionNode
}

export interface OutputNode extends SourceLocation {
  kind: 'output'
  values: ExpressionNode[]
}

export interface InputNode extends SourceLocation {
  kind: 'input'
  targets: ExpressionNode[]
}

export interface BreakNode extends SourceLocation { kind: 'break' }
export interface ContinueNode extends SourceLocation { kind: 'continue' }

export type ExpressionNode = LiteralNode | IdentifierNode | BinaryNode | UnaryNode | AssignmentNode | ArrayAccessNode | CallNode | MemberCallNode

export interface LiteralNode extends SourceLocation {
  kind: 'literal'
  value: string | number | boolean
  literalType: PrimitiveType
}

export interface IdentifierNode extends SourceLocation { kind: 'identifier'; name: string }

export interface BinaryNode extends SourceLocation {
  kind: 'binary'
  operator: string
  left: ExpressionNode
  right: ExpressionNode
}

export interface UnaryNode extends SourceLocation {
  kind: 'unary'
  operator: string
  argument: ExpressionNode
  postfix: boolean
}

export interface AssignmentNode extends SourceLocation {
  kind: 'assignment'
  target: IdentifierNode | ArrayAccessNode
  operator: '=' | '+=' | '-='
  value: ExpressionNode
}

export interface ArrayAccessNode extends SourceLocation {
  kind: 'array_access'
  target: IdentifierNode
  indices: ExpressionNode[]
}

export interface CallNode extends SourceLocation {
  kind: 'call'
  callee: string
  arguments: ExpressionNode[]
}

export interface MemberCallNode extends SourceLocation {
  kind: 'member_call'
  target: IdentifierNode
  member: string
  arguments: ExpressionNode[]
}

export type RuntimeValue = RuntimeInteger | RuntimeDouble | RuntimeBoolean | RuntimeCharacter | RuntimeString | RuntimeArray | RuntimeArray2D | RuntimeVoid
export interface RuntimeInteger { kind: 'int'; value: number }
export interface RuntimeDouble { kind: 'double'; value: number }
export interface RuntimeBoolean { kind: 'bool'; value: boolean }
export interface RuntimeCharacter { kind: 'char'; value: string }
export interface RuntimeString { kind: 'string'; value: string }
export interface RuntimeArray { kind: 'array'; elementType: PrimitiveType; length: number; values: RuntimeValue[] }
export interface RuntimeArray2D { kind: 'array2d'; elementType: PrimitiveType; rows: number; columns: number; values: RuntimeValue[][] }
export interface RuntimeVoid { kind: 'void' }

export type TraceEventType =
  | 'program_start' | 'program_end' | 'line_enter' | 'scope_enter' | 'scope_exit'
  | 'variable_declare' | 'variable_read' | 'variable_assign' | 'array_declare' | 'array_read' | 'array_write'
  | 'array_2d_declare' | 'array_2d_read' | 'array_2d_write' | 'string_declare' | 'string_read' | 'string_write'
  | 'binary_operation' | 'comparison' | 'branch' | 'loop_enter' | 'loop_condition' | 'loop_iteration'
  | 'loop_update' | 'loop_exit' | 'function_enter' | 'function_return' | 'console_input' | 'console_output' | 'runtime_error'

export interface BaseTraceEvent {
  id: number
  type: TraceEventType
  line: number
  column?: number
  description: string
  scopeId: string
  data?: Record<string, string | number | boolean | string[]>
}

export interface RuntimeVariable {
  name: string
  dataType: PrimitiveType
  value: RuntimeValue
  scopeId: string
  scopeName: string
  changed: boolean
  previousValue?: RuntimeValue
  read?: boolean
}

export interface RuntimeScopeSnapshot { id: string; name: string }
export interface RuntimeStackFrame { id: string; name: string; line: number; active: boolean }
export interface LoopContext { id: string; type: 'for' | 'while'; iteration: number; line: number; condition: string; result: boolean }

export interface ExecutionSnapshot {
  variables: RuntimeVariable[]
  callStack: RuntimeStackFrame[]
  scopes: RuntimeScopeSnapshot[]
  consoleOutput: string[]
  currentLine: number
  loops: LoopContext[]
}

export interface ExecutionResult {
  events: BaseTraceEvent[]
  snapshots: ExecutionSnapshot[]
  treeSummary: string
}

export interface ParseError { line: number; column: number; message: string }
export interface RuntimeErrorInfo { line: number; message: string }

export interface ExecutionLimits {
  maxSteps: number
  maxLoopIterations: number
  maxArrayLength: number
  maxMatrixCells: number
  maxCallDepth: number
  timeoutMs: number
}

export const executionLimits: ExecutionLimits = {
  maxSteps: 10000,
  maxLoopIterations: 2000,
  maxArrayLength: 500,
  maxMatrixCells: 2500,
  maxCallDepth: 30,
  timeoutMs: 5000
}

export type WorkerRequest = { type: 'validate'; code: string } | { type: 'run'; code: string; input: string[] }
export type WorkerResponse =
  | { type: 'parse_success'; treeSummary: string }
  | { type: 'parse_error'; errors: ParseError[] }
  | { type: 'run_complete'; result: ExecutionResult }
  | { type: 'runtime_error'; error: RuntimeErrorInfo }
