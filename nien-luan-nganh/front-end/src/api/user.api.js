import { http } from './http'

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

