import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export interface PetInfo {
  id: number
  name: string
  description?: string
  published: boolean
  owned: boolean
  hasPreview: boolean
  hasAnimation: boolean
}
export interface AdminPetInfo extends Omit<PetInfo, 'owned'> { personalityPrompt: string }
export interface ChatMessage { role: 'user' | 'assistant'; content: string }

export const listPetsApi = () => request.get<PetInfo[], PetInfo[]>('/pets')
export const listAdminPetsApi = () => request.get<AdminPetInfo[], AdminPetInfo[]>('/pets/admin')
export const createPetApi = (data: { name: string; description: string; personalityPrompt: string }) => request.post<AdminPetInfo, AdminPetInfo>('/pets/admin', data)
export const updatePetApi = (id: number, data: { name: string; description: string; personalityPrompt: string }) => request.put<AdminPetInfo, AdminPetInfo>(`/pets/admin/${id}`, data)
export const publishPetApi = (id: number, published: boolean) => request.put<AdminPetInfo, AdminPetInfo>(`/pets/admin/${id}/publish`, { published })
export const uploadPetAssetApi = (id: number, kind: 'preview' | 'atlas', file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request.post<AdminPetInfo, AdminPetInfo>(`/pets/admin/${id}/assets/${kind}`, data, { timeout: 60000 })
}
export const petGrantsApi = (studentId: number) => request.get<number[], number[]>(`/pets/teacher/students/${studentId}/grants`)
export const setPetGrantApi = (studentId: number, petId: number, granted: boolean) => request.put<void, void>(`/pets/teacher/students/${studentId}/grants/${petId}`, { granted })
export const petChatApi = (petId: number, messages: ChatMessage[]) => request.post<{ reply: string }, { reply: string }>(`/pets/${petId}/chat`, { messages }, { timeout: 55000 })

export async function petMediaUrl(petId: number, kind: 'preview' | 'atlas'): Promise<string> {
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/pets/${petId}/media/${kind}`, {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
  if (!response.ok || !(response.headers.get('content-type') || '').startsWith('image/')) {
    throw new Error('宠物图片加载失败')
  }
  return URL.createObjectURL(await response.blob())
}
