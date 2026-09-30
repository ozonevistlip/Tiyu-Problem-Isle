export type UserRole = 'teacher' | 'student' | 'SUPER_ADMIN'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  username: string
  realName?: string
  nickname?: string
  role: UserRole
  status: number
  lastLoginAt?: string
  lastActiveAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AdminDashboard {
  totalUsers: number
  teachers: number
  students: number
  disabledUsers: number
  onlineUsers: number
  totalVisits: number
  todayVisits: number
  uptimeSeconds: number
  processors: number
  maxMemoryMb: number
  usedMemoryMb: number
  systemLoadAverage: number
}

export interface OnlineSession {
  id: string
  userId: number
  username: string
  realName?: string
  role: UserRole
  ipAddress?: string
  userAgent?: string
  loginAt: string
  lastActiveAt: string
}

export interface Announcement {
  id: number
  title: string
  content: string
  status: number
  createdBy: number
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface ClassInfo {
  id: number
  teacherId: number
  className: string
  description?: string
  status: number
  createdAt?: string
}

export interface ProblemInfo {
  id: number
  title: string
  description: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  difficulty?: string
  timeLimitMs?: number
  memoryLimitMb?: number
  compareMode?: string
  visibility?: string
  acceptedCount?: number
  submitCount?: number
}

export interface TestcaseInfo {
  id: number
  problemId: number
  inputData: string
  outputData: string
  score: number
  sortOrder: number
  sample: boolean
}

export interface HintInfo {
  id: number
  problemId: number
  hintTitle?: string
  hintContent: string
  hintLevel: number
}

export interface ContestInfo {
  id: number
  teacherId: number
  classId: number
  title: string
  description?: string
  startTime: string
  endTime: string
  status: 'DRAFT' | 'PUBLISHED' | 'FINISHED' | 'CANCELLED'
  showRank: boolean
  allowSubmitAfterEnd: boolean
}

export interface ContestProblemInfo {
  problemId: number
  title: string
  description?: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  difficulty?: string
  displayOrder: number
  score: number
  hintUnlockMinutes: number
  status?: 'UNTRIED' | 'TRIED' | 'ACCEPTED' | 'PARTIAL'
  bestScore?: number
  submitCount?: number
  canShowHint?: boolean
  hintUnlockRemainSeconds?: number
}

export interface HintView {
  canShowHint: boolean
  unlockRemainSeconds: number
  hintTitle?: string
  hintContent?: string
  hintLevel?: number
  hintShown: boolean
}

export interface SubmissionInfo {
  id: number
  userId: number
  problemId: number
  contestId?: number
  language: string
  status: string
  score: number
  timeUsedMs: number
  memoryUsedKb: number
  errorMessage?: string
  judgedAt?: string
  createdAt?: string
}

export interface CustomTestResult {
  status: string
  stdout: string
  stderr: string
  timeUsedMs: number
  errorMessage?: string
}

export interface SubmissionCaseInfo {
  id: number
  testcaseId: number
  status: string
  timeUsedMs: number
  memoryUsedKb: number
  inputPreview?: string
  expectedOutputPreview?: string
  actualOutputPreview?: string
  errorMessage?: string
}

export interface RankInfo {
  studentId: number
  studentName?: string
  totalScore: number
  acceptedCount: number
  submitCount: number
  lastSubmitAt?: string
}
