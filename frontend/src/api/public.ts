import request from '@/utils/request'
import type { Announcement } from './types'

export const publicConfigApi = () => request.get<{ registrationEnabled: boolean }, { registrationEnabled: boolean }>('/public/config')
export const publicAnnouncementsApi = () => request.get<Announcement[], Announcement[]>('/public/announcements')
