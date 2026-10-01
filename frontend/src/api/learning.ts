import request from '@/utils/request'

export type Tier = 'BASIC' | 'GROWTH' | 'CHALLENGE'
export const tierNames: Record<Tier, string> = { BASIC: '基础', GROWTH: '成长', CHALLENGE: '挑战' }

export interface ClassType { id: number; name: string; description?: string; status: number }
export interface Lesson { id: number; classTypeId: number; title: string; lessonOrder: number; status: number }
export interface Material { id: number; lessonId?: number; tier?: Tier; kind: 'VIDEO' | 'HOMEWORK'; title: string; content?: string }
export interface MyClass { classId: number; className: string; classTypeName: string }
export interface MyLesson { id: number; title: string; lessonOrder: number; tier: Tier | null; materials: Material[] }

export const listTypesApi = () => request.get<ClassType[], ClassType[]>('/learning/types')
export const createTypeApi = (data: { name: string; description?: string }) => request.post<ClassType, ClassType>('/learning/types', data)
export const updateTypeApi = (id: number, data: { name: string; description?: string }) => request.put<ClassType, ClassType>(`/learning/types/${id}`, data)
export const deleteTypeApi = (id: number) => request.delete<void, void>(`/learning/types/${id}`)
export const listLessonsApi = (typeId: number) => request.get<Lesson[], Lesson[]>(`/learning/types/${typeId}/lessons`)
export const createLessonApi = (typeId: number, data: { title: string; lessonOrder: number; status: number }) => request.post<Lesson, Lesson>(`/learning/types/${typeId}/lessons`, data)
export const updateLessonApi = (id: number, data: { title: string; lessonOrder: number; status: number }) => request.put<Lesson, Lesson>(`/learning/lessons/${id}`, data)
export const deleteLessonApi = (id: number) => request.delete<void, void>(`/learning/lessons/${id}`)
export const listMaterialsApi = (lessonId: number) => request.get<Material[], Material[]>(`/learning/lessons/${lessonId}/materials`)
export const createHomeworkApi = (lessonId: number, data: { title: string; tier: Tier; content: string }) => request.post<Material, Material>(`/learning/lessons/${lessonId}/homework`, data)
export const uploadVideoApi = (lessonId: number, data: FormData) => request.post<Material, Material>(`/learning/lessons/${lessonId}/videos`, data, { timeout: 600000 })
export const deleteMaterialApi = (id: number) => request.delete<void, void>(`/learning/materials/${id}`)
export const myClassesApi = () => request.get<MyClass[], MyClass[]>('/learning/my-classes')
export const myLessonsApi = (classId: number) => request.get<MyLesson[], MyLesson[]>(`/learning/my-classes/${classId}/lessons`)

export async function videoStreamUrl(id: number): Promise<string> {
  const result = await request.get<{ ticket: string }, { ticket: string }>(`/learning/videos/${id}/ticket`)
  return `${import.meta.env.VITE_API_BASE_URL}/learning/stream/${encodeURIComponent(result.ticket)}`
}
