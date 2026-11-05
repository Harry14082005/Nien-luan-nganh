import { http } from './http'

export const resolveFileUrl = (storageKey) => {
    if (!storageKey) return '';
    if (storageKey.startsWith('http://') || storageKey.startsWith('https://') || storageKey.startsWith('blob:')) {
        return storageKey;
    }
    const cleanKey = storageKey.startsWith('/') ? storageKey.slice(1) : storageKey;
    const baseURL = http.defaults.baseURL || '/api/v1';
    return `${baseURL}/files?storageKey=${encodeURIComponent(cleanKey)}`;
};

// Lưu cache các blob đã tải để tránh gọi lại liên tục gây nặng RAM
const blobUrlCache = new Map();

/**
 * Gọi API tải file qua Axios có đính kèm Token, trả về blob URL an toàn cho thẻ img/video/a
 */
export const loadFileAsBlobUrl = async (storageKey) => {
    if (!storageKey) return '';
    if (storageKey.startsWith('http://') || storageKey.startsWith('https://') || storageKey.startsWith('blob:')) {
        return storageKey;
    }

    if (blobUrlCache.has(storageKey)) {
        return blobUrlCache.get(storageKey);
    }

    try {
        const cleanKey = storageKey.startsWith('/') ? storageKey.slice(1) : storageKey;
        const response = await http.get('/files', {
            params: { storageKey: cleanKey },
            responseType: 'blob'
        });

        const blobUrl = URL.createObjectURL(response.data);
        blobUrlCache.set(storageKey, blobUrl);
        return blobUrl;
    } catch (error) {
        console.error("Lỗi tải file kèm token:", error);
        return '';
    }
};