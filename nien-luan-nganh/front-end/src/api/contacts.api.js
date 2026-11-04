import { http } from './http'

export const getFriends = ({ page = 0, size = 100 } = {}) =>
  http.get('/friendships', { params: { status: 'ACCEPTED', page, size } }).then((r) => r.data.data)

export const getPendingFriendships = ({ page = 0, size = 50 } = {}) =>
  http.get('/friendships', { params: { status: 'PENDING', page, size } }).then((r) => r.data.data)

export const sendFriendRequest = (friendId) =>
  http.post('/friendships/request', { friendId }).then((r) => r.data.data)

export const acceptedFriendship = (friendshipId) =>
  http.put(`/friendships/${friendshipId}/accept`).then((r) => r.data.data)
export const declineFriendship = (friendshipId) =>
  http.put(`/friendships/${friendshipId}/decline`).then((r) => r.data.data)
export const removeFriendship = (friendshipId) =>
  http.delete(`/friendships/${friendshipId}`).then((r) => r.data)
// Coi là online nếu lastSeenAt trong vòng 5 phút gần nhất
const ONLINE_THRESHOLD_MS = 5 * 60 * 1000

function isOnline(lastSeenAt) {
  if (!lastSeenAt) return false
  const t = new Date(lastSeenAt).getTime()
  if (Number.isNaN(t)) return false
  return Date.now() - t < ONLINE_THRESHOLD_MS
}

function formatLastSeen(lastSeenAt) {
  if (!lastSeenAt) return 'Ngoại tuyến'
  const t = new Date(lastSeenAt).getTime()
  if (Number.isNaN(t)) return 'Ngoại tuyến'

  const minutes = Math.floor((Date.now() - t) / 60000)
  if (minutes < 1) return 'Vừa mới rời đi'
  if (minutes < 60) return `Hoạt động ${minutes} phút trước`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `Hoạt động ${hours} giờ trước`

  const days = Math.floor(hours / 24)
  return `Hoạt động ${days} ngày trước`
}
export function toContact(friendship) {
  const u = friendship.friend
  const online = isOnline(u.lastSeenAt)
  return {
    id: u.userId, // dùng userId để chọn item và tạo phòng chat
    friendshipId: friendship.friendshipId,
    name: u.username,
    role: u.email || u.phoneNumber || '', // API không có chức danh nghề nghiệp
    avatar: u.avatarUrl || '',
    online,
    statusText: online ? 'Đang hoạt động' : formatLastSeen(u.lastSeenAt),
    bio: u.description || '',
  }
}
