import { http } from "./http";

export const register = (body) => http.post("/auth/register", body).then((r) => r.data.result);
export const login = (body) => http.post("/auth/login", body).then((r) => r.data.result);
export const logout = (refreshToken) => http.post("/auth/logout", { refreshToken });
export const getMe = () => http.get("/users/me").then((r) => r.data.result);