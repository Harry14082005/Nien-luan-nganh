import { http } from './http'

const BASE_URL = import.meta.env.VITE_API_URL || '/api/v1'

/**
 * Tạo URL đầy đủ để load ảnh public từ storageKey / fileName.
 * @param {string} storageKey 
 */
export const getImageUrl = (storageKey) => {
  if (!storageKey) return ''
  if (storageKey.startsWith('http')) return storageKey

  // Nếu storageKey có dạng "avatars/filename.jpg", ta chỉ lấy phần tên file phía sau
  const fileName = storageKey.includes('/') ? storageKey.split('/').pop() : storageKey

  // Trỏ đúng vào endpoint @GetMapping("/{fileName:.+}") của Backend
  return `${BASE_URL}/files/${encodeURIComponent(fileName)}`
}
/**
 * Tải ảnh riêng tư cần kèm token và chuyển thành Object URL để hiển thị vào thẻ img.
 * @param {string} storageKey 
 * @returns {Promise<string>} Blob URL tạm thời
 */
export const fetchPrivateImageUrl = async (storageKey) => {
  if (!storageKey) return ''
  if (storageKey.startsWith('http')) return storageKey

  try {
    const response = await http.get('/files', {
      params: { storageKey },
      responseType: 'blob' // Bắt buộc để nhận dữ liệu file binary
    })
    return URL.createObjectURL(response.data)
  } catch (err) {
    console.error('Không thể tải ảnh riêng tư:', err)
    return ''
  }
}

/**
 * Upload avatar mới cho người dùng hiện tại.
 * @param {File} file File ảnh cần upload
 * @returns {Promise<UserResponse>} Thông tin user sau khi cập nhật
 */
export const uploadAvatar = (file) => {
  const form = new FormData()
  form.append('file', file)
  return http
    .post('/users/me/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}


/**
 * Lấy thông tin chi tiết một người dùng theo userId.
 * @param {string} userId
 */
export const getUserById = (userId) => {
  return http.get(`/users/${userId}`).then((res) => res.data?.data ?? res.data)
}

/**
 * Tìm kiếm người dùng theo Tên hiển thị, Email hoặc Số điện thoại.
 * @param {string} q Từ khóa tìm kiếm
 * @param {number} page Trang hiện tại (mặc định 0)
 * @param {number} size Số lượng kết quả (mặc định 20)
 */
export const searchUsers = (q, page = 0, size = 20) => {
  return http
    .get('/users/search', { params: { q, page, size } })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách người dùng gợi ý ban đầu (kết hợp tìm theo '@' và '0').
 */
export const getSuggestedUsers = async () => {
  try {
    const [byEmail, byPhone] = await Promise.all([
      searchUsers('@', 0, 20).catch(() => ({ content: [] })),
      searchUsers('0', 0, 20).catch(() => ({ content: [] })),
    ])
    const list = [...(byEmail?.content || []), ...(byPhone?.content || [])]
    const seen = new Set()
    return list.filter((u) => {
      if (!u || !u.userId || seen.has(u.userId)) return false
      seen.add(u.userId)
      return true
    })
  } catch {
    return []
  }
}

