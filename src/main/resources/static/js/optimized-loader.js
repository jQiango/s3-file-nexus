/**
 * 优化的文件加载器
 * 使用新的 /api/storage/v2/list API
 * 支持无限滚动、Redis缓存、ETag
 */

// 将此对象的方法合并到Vue实例中
window.OptimizedLoader = {
    data() {
        return {
            // 无限滚动相关
            allItems: [],              // 所有已加载的项目
            continuationToken: null,   // 分页token
            hasMore: true,            // 是否还有更多数据
            loadingMore: false,       // 是否正在加载更多
            useV2API: true,           // 使用v2 API
        };
    },

    methods: {
        /**
         * 加载文件列表（优化版）
         * @param {boolean} reset - 是否重置列表
         */
        async loadFilesV2(reset = true) {
            if (!this.currentBucket) {
                console.warn('未选择bucket');
                return;
            }

            // 重置加载
            if (reset) {
                this.allItems = [];
                this.continuationToken = null;
                this.hasMore = true;
            }

            // 防止重复加载
            if (this.loading || this.loadingMore || !this.hasMore) {
                return;
            }

            const isInitialLoad = this.allItems.length === 0;
            if (isInitialLoad) {
                this.loading = true;
            } else {
                this.loadingMore = true;
            }

            try {
                // 构建请求参数
                const params = new URLSearchParams({
                    bucket: this.currentBucket,
                    prefix: this.currentPath || '',
                    pageSize: 100
                });

                if (this.continuationToken) {
                    params.append('continuationToken', this.continuationToken);
                }

                console.log(`🔄 加载文件: ${this.currentPath || '/'} (reset=${reset}, token=${this.continuationToken ? 'yes' : 'no'})`);

                const response = await axios.get(`/api/storage/v2/list?${params}`);

                if (response.data.success) {
                    const data = response.data.data;

                    // 显示缓存来源
                    const cacheMsg = data.fromCache ? '✅ Caffeine缓存' : '💾 S3';
                    console.log(`来自: ${cacheMsg}`);

                    // 追加新数据
                    const newItems = data.items || [];
                    this.allItems.push(...newItems);
                    console.log(`✅ 加载了 ${newItems.length} 项，总计 ${this.allItems.length} 项`);

                    // 更新分页状态
                    this.continuationToken = data.nextContinuationToken;
                    this.hasMore = data.isTruncated || false;

                    if (this.hasMore) {
                        console.log('📄 还有更多数据');
                    } else {
                        console.log('✔️ 已加载全部数据');
                    }

                    // 更新显示
                    this.updateDisplayFiles();

                } else {
                    console.error('❌ API返回错误:', response.data.message);
                    this.showToast('error', response.data.message || '加载失败');
                }

            } catch (error) {
                console.error('❌ 加载失败:', error);
                this.handleConfigError({ message: error.message });
                this.showToast('error', '无法加载文件列表: ' + error.message);
            } finally {
                this.loading = false;
                this.loadingMore = false;
            }
        },

        /**
         * 更新显示的文件列表
         */
        updateDisplayFiles() {
            // 转换为旧格式（兼容现有filteredFiles计算属性）
            this.files = this.allItems.map(item => ({
                key: item.key,
                name: item.name,
                size: item.size || 0,
                lastModified: item.lastModified,
                isFolder: item.type === 'folder',
                folderStats: item.folderStats,
                // 保留原有字段
                storageClass: item.storageClass
            }));

            this.totalFiles = this.files.length;
            console.log(`📊 显示 ${this.files.length} 项`);
        },

        /**
         * 导航到指定路径
         */
        navigateToPathV2(path) {
            console.log(`🧭 导航到: ${path || '/'}`);
            this.currentPath = path;
            this.loadFilesV2(true); // 重置并重新加载
        },

        /**
         * 设置无限滚动
         */
        setupInfiniteScrollV2() {
            // 查找文件列表容器（网格或列表模式）
            const containers = [
                document.querySelector('.grid-view'),
                document.querySelector('.list-view'),
                document.querySelector('[class*="file-list"]'),
                document.querySelector('.overflow-y-auto')
            ];

            const container = containers.find(el => el !== null);

            if (!container) {
                console.warn('⚠️ 找不到文件列表容器，无限滚动未启用');
                console.log('提示：确保HTML中有 .grid-view 或 .list-view 类');
                return;
            }

            console.log('✅ 无限滚动已启用');

            // 节流滚动事件
            let scrollTimeout = null;
            let lastScrollTop = 0;

            const scrollHandler = () => {
                if (scrollTimeout) clearTimeout(scrollTimeout);

                scrollTimeout = setTimeout(() => {
                    const { scrollTop, scrollHeight, clientHeight } = container;

                    // 只在向下滚动时触发
                    if (scrollTop < lastScrollTop) {
                        lastScrollTop = scrollTop;
                        return;
                    }
                    lastScrollTop = scrollTop;

                    const distanceToBottom = scrollHeight - scrollTop - clientHeight;

                    // 距离底部200px时触发加载
                    if (distanceToBottom < 200 && this.hasMore && !this.loadingMore && !this.loading) {
                        console.log('🔻 触发无限滚动加载 (距底部 ' + Math.round(distanceToBottom) + 'px)');
                        this.loadFilesV2(false); // false = 不重置，追加加载
                    }
                }, 150);
            };

            container.addEventListener('scroll', scrollHandler);
            console.log('👂 监听滚动事件');

            // 返回清理函数
            return () => {
                container.removeEventListener('scroll', scrollHandler);
                console.log('🔇 移除滚动监听');
            };
        },

        /**
         * 切换bucket（重写）
         */
        async selectBucketV2(bucket) {
            console.log(`🪣 切换bucket: ${bucket}`);
            this.currentBucket = bucket;
            localStorage.setItem('currentBucket', bucket);
            this.currentPath = '';

            // 重置所有筛选条件
            this.filters.fileType = '';
            this.filters.startDate = '';
            this.filters.endDate = '';
            this.searchKeyword = '';
            this.showAdvancedFilters = false;

            // 重置日历状态
            const today = new Date();
            this.startCalendarYear = today.getFullYear();
            this.startCalendarMonth = today.getMonth() + 1;
            this.endCalendarYear = today.getFullYear();
            this.endCalendarMonth = today.getMonth() + 1;

            await this.loadFilesV2(true);
        },

        /**
         * 刷新当前目录
         */
        async refreshV2() {
            console.log('🔄 刷新当前目录');
            await this.loadFilesV2(true);
        },

        /**
         * 初始化V2 API
         */
        initV2API() {
            console.log('🚀 初始化优化API (v2)');

            // 替换方法
            this.loadFiles = this.loadFilesV2;
            this.navigateToPath = this.navigateToPathV2;
            this.selectBucket = this.selectBucketV2;
            this.refresh = this.refreshV2;

            // 设置无限滚动
            this.$nextTick(() => {
                this.setupInfiniteScrollV2();
            });

            console.log('✅ V2 API 初始化完成');
        },

        /**
         * 切换回旧API（用于对比测试）
         */
        switchToLegacyAPI() {
            console.log('⏮️ 切换回旧API');
            this.useV2API = false;
            // 这里需要恢复原有的loadFiles等方法
            // 实际使用时需要保存原方法的引用
        }
    },

    mounted() {
        // 在Vue实例mounted时自动初始化
        if (this.useV2API) {
            this.initV2API();
        }
    }
};

console.log('✅ OptimizedLoader 已加载');
console.log('使用方法: 在Vue实例的data和methods中混入 OptimizedLoader');
