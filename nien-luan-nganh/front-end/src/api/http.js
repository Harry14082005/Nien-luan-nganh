import axios from "axios";

export const http = axios.create({ baseURL: import.meta.env.VITE_API_URL });

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("accessToken");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const { response, config } = err;
    const isAuthCall = config?.url?.startsWith("/auth/");
    if (response?.status === 401 && !isAuthCall && !config._retry) {
      config._retry = true;
      try {
        const { data } = await axios.post(`${import.meta.env.VITE_API_URL}/auth/refresh`, {
          refreshToken: localStorage.getItem("refreshToken"),
        });
        localStorage.setItem("accessToken", data.result.accessToken);
        return http(config);
      } catch {
        localStorage.clear();
        location.href = "/login";
      }
    }
    return Promise.reject(err);
  }
);