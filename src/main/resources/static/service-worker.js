// Service Worker for S3 File Manager
// 版本号 - 更新此版本号会触发Service Worker更新
const CACHE_VERSION = 'v1.0.0';
const CACHE_NAME = `s3-file-manager-${CACHE_VERSION}`;

// 需要缓存的静态资源
const STATIC_ASSETS = [
    '/',
    '/index.html',
    '/favicon.svg',
    '/favicon.ico',
    // CDN资源会被浏览器自动缓存，这里主要缓存本地资源
];

// 需要缓存的API请求路径模式
const API_CACHE_PATTERNS = [
    '/api/storage/buckets',
    '/api/storage/backend',
];

// 安装事件 - 缓存静态资源
self.addEventListener('install', (event) => {
    console.log('[Service Worker] 安装中...', CACHE_VERSION);

    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('[Service Worker] 缓存静态资源');
                return cache.addAll(STATIC_ASSETS);
            })
            .then(() => {
                console.log('[Service Worker] 安装完成');
                // 强制激活新的Service Worker
                return self.skipWaiting();
            })
            .catch((error) => {
                console.error('[Service Worker] 安装失败:', error);
            })
    );
});

// 激活事件 - 清理旧缓存
self.addEventListener('activate', (event) => {
    console.log('[Service Worker] 激活中...', CACHE_VERSION);

    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames.map((cacheName) => {
                        if (cacheName !== CACHE_NAME) {
                            console.log('[Service Worker] 删除旧缓存:', cacheName);
                            return caches.delete(cacheName);
                        }
                    })
                );
            })
            .then(() => {
                console.log('[Service Worker] 激活完成');
                // 立即控制所有页面
                return self.clients.claim();
            })
    );
});

// Fetch事件 - 拦截网络请求
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // 只处理同源请求
    if (url.origin !== location.origin) {
        return;
    }

    // 根据请求类型选择缓存策略
    if (shouldCacheAPI(url.pathname)) {
        // API请求：网络优先，失败时使用缓存
        event.respondWith(networkFirstStrategy(request));
    } else if (shouldCacheStatic(url.pathname)) {
        // 静态资源：缓存优先
        event.respondWith(cacheFirstStrategy(request));
    } else {
        // 其他请求：仅网络
        event.respondWith(fetch(request));
    }
});

// 判断是否应该缓存API请求
function shouldCacheAPI(pathname) {
    return API_CACHE_PATTERNS.some(pattern => pathname.startsWith(pattern));
}

// 判断是否应该缓存静态资源
function shouldCacheStatic(pathname) {
    return pathname === '/' ||
           pathname.endsWith('.html') ||
           pathname.endsWith('.css') ||
           pathname.endsWith('.js') ||
           pathname.endsWith('.svg') ||
           pathname.endsWith('.ico') ||
           pathname.endsWith('.png') ||
           pathname.endsWith('.jpg') ||
           pathname.endsWith('.jpeg');
}

// 网络优先策略 - 优先从网络获取，失败时使用缓存
async function networkFirstStrategy(request) {
    try {
        const response = await fetch(request);

        // 只缓存成功的GET请求
        if (request.method === 'GET' && response.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, response.clone());
        }

        return response;
    } catch (error) {
        console.log('[Service Worker] 网络请求失败，尝试使用缓存:', request.url);
        const cachedResponse = await caches.match(request);

        if (cachedResponse) {
            return cachedResponse;
        }

        // 如果缓存也没有，返回离线页面
        return new Response(
            JSON.stringify({
                success: false,
                message: '网络连接失败，请检查网络后重试',
                offline: true
            }),
            {
                status: 503,
                statusText: 'Service Unavailable',
                headers: { 'Content-Type': 'application/json' }
            }
        );
    }
}

// 缓存优先策略 - 优先使用缓存，缓存未命中时从网络获取
async function cacheFirstStrategy(request) {
    const cachedResponse = await caches.match(request);

    if (cachedResponse) {
        // 后台更新缓存
        fetch(request).then((response) => {
            if (response.ok) {
                caches.open(CACHE_NAME).then((cache) => {
                    cache.put(request, response);
                });
            }
        }).catch(() => {
            // 忽略后台更新失败
        });

        return cachedResponse;
    }

    try {
        const response = await fetch(request);

        if (response.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, response.clone());
        }

        return response;
    } catch (error) {
        console.error('[Service Worker] 缓存优先策略失败:', error);

        // 返回离线提示
        if (request.destination === 'document') {
            return new Response(
                `<!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>离线模式</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                        }
                        .container {
                            text-align: center;
                            padding: 40px;
                            background: rgba(255, 255, 255, 0.1);
                            border-radius: 20px;
                            backdrop-filter: blur(10px);
                        }
                        h1 { font-size: 48px; margin-bottom: 20px; }
                        p { font-size: 18px; margin-bottom: 30px; }
                        button {
                            padding: 15px 30px;
                            font-size: 16px;
                            background: white;
                            color: #667eea;
                            border: none;
                            border-radius: 10px;
                            cursor: pointer;
                            font-weight: bold;
                        }
                        button:hover { transform: scale(1.05); }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>📡 离线模式</h1>
                        <p>当前网络连接不可用</p>
                        <p>请检查您的网络连接后重试</p>
                        <button onclick="location.reload()">重新加载</button>
                    </div>
                </body>
                </html>`,
                {
                    status: 503,
                    statusText: 'Service Unavailable',
                    headers: { 'Content-Type': 'text/html; charset=utf-8' }
                }
            );
        }

        throw error;
    }
}

// 监听消息事件 - 用于手动清除缓存
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'CLEAR_CACHE') {
        event.waitUntil(
            caches.keys().then((cacheNames) => {
                return Promise.all(
                    cacheNames.map((cacheName) => caches.delete(cacheName))
                );
            }).then(() => {
                console.log('[Service Worker] 缓存已清除');
                event.ports[0].postMessage({ success: true });
            })
        );
    }

    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});
