import { http, HttpResponse, delay } from "msw";
import { ERR } from "../errors";

const BASE = import.meta.env.VITE_API_URL;

const users = [
  { id: 1, username: "An", email: "an@test.com", phone: "0900000001", password: "123456", role: "USER", isBanned: false },
  { id: 2, username: "Bị khóa", email: "ban@test.com", phone: "0900000002", password: "123456", role: "USER", isBanned: true },
];

const ok = (result, status = 200) => HttpResponse.json({ code: 1000, result }, { status });
const fail = ({ status, ...body }) => HttpResponse.json(body, { status });
const publicUser = ({ password, ...u }) => u;

export const authHandlers = [
  http.post(`${BASE}/auth/register`, async ({ request }) => {
  await delay(500)
  const { username, identifier, password, confirmPassword, gender, preferredLanguage } =
    await request.json()
  if (password !== confirmPassword) return fail(ERR.PASSWORD_NOT_MATCH)

  const isEmail = identifier?.includes('@')
  if (isEmail && users.some((u) => u.email === identifier)) return fail(ERR.EMAIL_EXISTED)
  if (!isEmail && users.some((u) => u.phone === identifier)) return fail(ERR.PHONE_NUMBER_EXISTED)

  const user = {
    id: users.length + 1,
    username,
    email: isEmail ? identifier : null,
    phone: isEmail ? null : identifier,
    password,
    gender,
    preferredLanguage,
    role: 'USER',
    isBanned: false,
  }
  users.push(user)
  return ok(
    { accessToken: `access-${user.id}`, refreshToken: `refresh-${user.id}`, user: publicUser(user) },
    201
  )
}),

  http.post(`${BASE}/auth/login`, async ({ request }) => {
    await delay(500);
    const { identifier, password } = await request.json();
    const user = users.find((u) => u.email === identifier || u.phone === identifier);
    if (!user || user.password !== password) return fail(ERR.INVALID_CREDENTIALS);
    if (user.isBanned) return fail(ERR.USER_BANNED);
    return ok({ accessToken: `access-${user.id}`, refreshToken: `refresh-${user.id}` });
  }),

  http.post(`${BASE}/auth/refresh`, async ({ request }) => {
    const { refreshToken } = await request.json();
    const id = refreshToken?.split("-")[1];
    if (!id) return fail(ERR.UNAUTHENTICATED);
    return ok({ accessToken: `access-${id}`, refreshToken });
  }),

  http.post(`${BASE}/auth/logout`, () => ok(null)),

  http.get(`${BASE}/users/me`, ({ request }) => {
    const id = request.headers.get("Authorization")?.split("access-")[1];
    const user = users.find((u) => String(u.id) === id);
    return user ? ok(publicUser(user)) : fail(ERR.UNAUTHENTICATED);
  }),
];