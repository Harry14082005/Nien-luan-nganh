import { http} from "./http";

export const getFriends =({page = 0,size = 100} = {})=>
    http
        .get("/friendships",{params:{status: "ACCEPTED",page,size}})
        .then((r)=>r.data.data);

export const getPendingFriendships=({page =0,size=50}={})=>
    http
        .get("/friendships",{params:{status:"PENDING",page,size}})
        .then((r)=>r.data.data);

export const sendFriendRequest = (friendId)=>
    http
        .post("/friendships/request",{friendId})
        .then((r)=>r.data.data)

export const acceptedFriendship =(friendshipId)=>
    http
        .post(`/friendships/${friendshipId}/accept`)
        .then((r)=>r.data.data)
export const declineFriendship = (friendshipId)=>
    http
        .put(`/friendships/${friendshipId}/decline`)
        .then((r)=>r.data.data)
export const removeFriendship = (friendshipId)=>
    http 
        .delete(`/friendships/${friendshipId}`)
        .then((r)=>r.data)
export function toContact(friendship) {
  const u = friendship.friend;
  const online = isOnline(u.lastSeenAt);
  return {
    id: u.userId, // dùng userId để chọn item và tạo phòng chat
    friendshipId: friendship.friendshipId,
    name: u.username,
    role: u.email || u.phoneNumber || "", // API không có chức danh nghề nghiệp
    avatar: u.avatarUrl || "",
    online,
    statusText: online ? "Đang hoạt động" : formatLastSeen(u.lastSeenAt),
    bio: u.description || "",
  };
}