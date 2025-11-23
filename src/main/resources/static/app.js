const { createApp, ref, computed, onMounted } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

// 注册所有图标
const icons = ElementPlusIconsVue;

const app = createApp({
    setup() {
        // 状态管理
        const buckets = ref([]);
        const currentBucket = ref('');
        const currentPath = ref('');
        const files = ref([]);
        const selectedFiles = ref([]);
        const loading = ref(false);
        const searchKeyword = ref('');
        const tableHeight = ref(600);

        // 分页状态 - 默认每页100条
        const pagination = ref({
            pageSize: 100,
            currentPage: 1,
            hasMore: false,
            nextContinuationToken: null,
            total: 0
        });

        // 对话框状态
        const uploadDialogVisible = ref(false);
        const folderDialogVisible = ref(false);
        const uploadFiles = ref([]);
        const uploading = ref(false);
        const folderName = ref('');
        const dragOver = ref(false);

        // 计算属性
        const pathParts = computed(() => {
            if (!currentPath.value) return [];
            return currentPath.value.split('/').filter(part => part);
        });

        const sortedFiles = computed(() => {
            const arr = [...files.value];
            // 文件夹永远在最上面
            return arr.sort((a, b) => {
                if (a.isFolder && !b.isFolder) return -1;
                if (!a.isFolder && b.isFolder) return 1;
                return a.name.localeCompare(b.name);
            });
        });

        const totalCount = computed(() => {
            return files.value.length + (pagination.value.hasMore ? '+' : '');
        });

        // 初始化
        onMounted(async () => {
            await loadBuckets();
            calculateTableHeight();
            window.addEventListener('resize', calculateTableHeight);
        });

        // 计算表格高度
        const calculateTableHeight = () => {
            const windowHeight = window.innerHeight;
            tableHeight.value = windowHeight - 380;
        };

        // 加载存储桶列表
        const loadBuckets = async () => {
            try {
                const response = await axios.get('/api/storage/buckets');
                if (response.data.success) {
                    buckets.value = response.data.data;
                    // 自动选择第一个bucket
                    if (buckets.value.length > 0) {
                        const savedBucket = localStorage.getItem('currentBucket');
                        if (savedBucket && buckets.value.includes(savedBucket)) {
                            currentBucket.value = savedBucket;
                        } else {
                            currentBucket.value = buckets.value[0];
                        }
                        await loadFiles();
                    }
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                console.error('加载存储桶失败:', error);
                showError('加载存储桶失败');
            }
        };

        // 切换Bucket
        const onBucketChange = () => {
            localStorage.setItem('currentBucket', currentBucket.value);
            currentPath.value = '';
            pagination.value.currentPage = 1;
            pagination.value.nextContinuationToken = null;
            loadFiles();
        };

        // 加载文件列表
        const loadFiles = async (pageChange = false) => {
            if (!currentBucket.value) return;

            loading.value = true;
            try {
                const requestData = {
                    bucketName: currentBucket.value,
                    prefix: currentPath.value,
                    delimiter: '/',
                    pageSize: pagination.value.pageSize
                };

                // 如果是翻页，添加continuationToken
                if (pageChange && pagination.value.nextContinuationToken) {
                    requestData.continuationToken = pagination.value.nextContinuationToken;
                }

                const response = await axios.post('/api/storage/files/list', requestData);

                if (response.data.success) {
                    const data = response.data.data;
                    const newFiles = [];

                    // 处理文件夹
                    if (data.folders && data.folders.length > 0) {
                        data.folders.forEach(folder => {
                            newFiles.push({
                                key: folder.key,
                                name: folder.name,
                                isFolder: true,
                                size: 0,
                                lastModified: new Date()
                            });
                        });
                    }

                    // 处理文件
                    if (data.files && data.files.length > 0) {
                        data.files.forEach(file => {
                            // 提取文件名
                            let fileName = file.key;
                            if (currentPath.value && fileName.startsWith(currentPath.value)) {
                                fileName = fileName.substring(currentPath.value.length);
                            }
                            if (fileName.startsWith('/')) {
                                fileName = fileName.substring(1);
                            }

                            // 只显示当前目录下的文件
                            if (fileName && !fileName.includes('/')) {
                                newFiles.push({
                                    key: file.key,
                                    name: fileName,
                                    size: file.size || 0,
                                    lastModified: file.lastModified,
                                    isFolder: false
                                });
                            }
                        });
                    }

                    files.value = newFiles;

                    // 更新分页信息
                    if (data.pagination) {
                        pagination.value.hasMore = data.pagination.hasMore;
                        pagination.value.nextContinuationToken = data.pagination.nextContinuationToken;
                        // 估算总数（用于分页组件）
                        pagination.value.total = pagination.value.currentPage * pagination.value.pageSize +
                            (data.pagination.hasMore ? pagination.value.pageSize : 0);
                    }

                    selectedFiles.value = [];
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                console.error('加载文件列表失败:', error);
                showError('加载文件列表失败');
            } finally {
                loading.value = false;
            }
        };

        // 处理分页变化
        const handlePageChange = (page) => {
            pagination.value.currentPage = page;
            loadFiles(true);
        };

        // 点击文件或文件夹
        const handleFileClick = (file) => {
            if (file.isFolder) {
                currentPath.value = file.key;
                pagination.value.currentPage = 1;
                pagination.value.nextContinuationToken = null;
                loadFiles();
            }
        };

        // 导航到指定路径
        const navigateToPath = (path) => {
            currentPath.value = path;
            pagination.value.currentPage = 1;
            pagination.value.nextContinuationToken = null;
            loadFiles();
        };

        // 获取到指定索引的路径
        const getPathUpTo = (index) => {
            const parts = currentPath.value.split('/').filter(part => part);
            if (index >= 0 && index < parts.length) {
                return parts.slice(0, index + 1).join('/') + '/';
            }
            return '';
        };

        // 选择变化
        const handleSelectionChange = (selection) => {
            selectedFiles.value = selection;
        };

        // 显示上传对话框
        const showUploadDialog = () => {
            uploadFiles.value = [];
            uploadDialogVisible.value = true;
        };

        // 文件选择
        const handleFileSelect = (event) => {
            const files = Array.from(event.target.files);
            uploadFiles.value.push(...files);
        };

        // 拖拽上传
        const handleFileDrop = (event) => {
            dragOver.value = false;
            const files = Array.from(event.dataTransfer.files);
            uploadFiles.value.push(...files);
        };

        // 移除待上传文件
        const removeUploadFile = (index) => {
            uploadFiles.value.splice(index, 1);
        };

        // 执行上传
        const doUploadFiles = async () => {
            if (uploadFiles.value.length === 0) return;

            uploading.value = true;
            let successCount = 0;
            let failCount = 0;

            for (const file of uploadFiles.value) {
                try {
                    const formData = new FormData();
                    formData.append('file', file);
                    formData.append('bucketName', currentBucket.value);

                    if (currentPath.value) {
                        formData.append('objectKey', currentPath.value + file.name);
                    }

                    const response = await axios.post('/api/storage/upload', formData, {
                        headers: { 'Content-Type': 'multipart/form-data' }
                    });

                    if (response.data.success) {
                        successCount++;
                    } else {
                        failCount++;
                        console.error(`上传失败: ${file.name}`, response.data.message);
                    }
                } catch (error) {
                    failCount++;
                    console.error(`上传失败: ${file.name}`, error);
                }
            }

            uploading.value = false;
            uploadDialogVisible.value = false;
            uploadFiles.value = [];

            if (successCount > 0) {
                showSuccess(`成功上传 ${successCount} 个文件`);
                await loadFiles();
            }
            if (failCount > 0) {
                showError(`${failCount} 个文件上传失败`);
            }
        };

        // 下载文件
        const downloadFile = async (file) => {
            try {
                const url = `/api/storage/download?bucketName=${encodeURIComponent(currentBucket.value)}&objectKey=${encodeURIComponent(file.key)}`;

                // 创建隐藏的a标签下载
                const link = document.createElement('a');
                link.href = url;
                link.download = file.name;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                showSuccess('开始下载文件');
            } catch (error) {
                console.error('下载文件失败:', error);
                showError('下载文件失败');
            }
        };

        // 删除文件
        const deleteFile = async (file) => {
            try {
                await ElMessageBox.confirm(
                    `确定要删除 ${file.name} 吗？此操作不可恢复。`,
                    '确认删除',
                    {
                        confirmButtonText: '删除',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }
                );

                const response = await axios.delete('/api/storage/files', {
                    params: {
                        bucketName: currentBucket.value,
                        objectKey: file.key
                    }
                });

                if (response.data.success) {
                    showSuccess('删除成功');
                    await loadFiles();
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                if (error !== 'cancel') {
                    console.error('删除文件失败:', error);
                    showError('删除文件失败');
                }
            }
        };

        // 批量删除
        const handleBatchDelete = async () => {
            if (selectedFiles.value.length === 0) return;

            try {
                await ElMessageBox.confirm(
                    `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？此操作不可恢复。`,
                    '确认批量删除',
                    {
                        confirmButtonText: '删除',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }
                );

                const objectKeys = selectedFiles.value.map(file => file.key);
                const response = await axios.delete('/api/storage/files/batch', {
                    data: {
                        bucketName: currentBucket.value,
                        objectKeys: objectKeys
                    }
                });

                if (response.data.success) {
                    showSuccess('批量删除成功');
                    await loadFiles();
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                if (error !== 'cancel') {
                    console.error('批量删除失败:', error);
                    showError('批量删除失败');
                }
            }
        };

        // 显示创建文件夹对话框
        const showCreateFolderDialog = () => {
            folderName.value = '';
            folderDialogVisible.value = true;
        };

        // 创建文件夹
        const createFolder = async () => {
            if (!folderName.value.trim()) {
                showError('请输入文件夹名称');
                return;
            }

            try {
                const folderPath = currentPath.value + folderName.value.trim() + '/';
                const response = await axios.post('/api/storage/folder', null, {
                    params: {
                        bucketName: currentBucket.value,
                        folderPath: folderPath
                    }
                });

                if (response.data.success) {
                    showSuccess('文件夹创建成功');
                    folderDialogVisible.value = false;
                    await loadFiles();
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                console.error('创建文件夹失败:', error);
                showError('创建文件夹失败');
            }
        };

        // 搜索文件
        const handleSearch = async () => {
            if (!searchKeyword.value.trim()) {
                showError('请输入搜索关键词');
                return;
            }

            loading.value = true;
            try {
                const response = await axios.get('/api/storage/search', {
                    params: {
                        bucketName: currentBucket.value,
                        keyword: searchKeyword.value.trim(),
                        prefix: currentPath.value,
                        maxResults: 100
                    }
                });

                if (response.data.success) {
                    const data = response.data.data;
                    const searchResults = data.files || [];

                    // 转换为显示格式
                    files.value = searchResults.map(file => {
                        let fileName = file.key;
                        if (currentPath.value && fileName.startsWith(currentPath.value)) {
                            fileName = fileName.substring(currentPath.value.length);
                        }
                        if (fileName.startsWith('/')) {
                            fileName = fileName.substring(1);
                        }

                        return {
                            key: file.key,
                            name: fileName,
                            size: file.size || 0,
                            lastModified: file.lastModified,
                            isFolder: false
                        };
                    });

                    showSuccess(`找到 ${searchResults.length} 个结果`);
                } else {
                    showError(response.data.message);
                }
            } catch (error) {
                console.error('搜索文件失败:', error);
                showError('搜索文件失败');
            } finally {
                loading.value = false;
            }
        };

        // 获取文件图标
        const getFileIcon = (file) => {
            if (file.isFolder) {
                return 'el-icon-folder folder-icon';
            }

            const ext = file.name.split('.').pop().toLowerCase();
            const iconMap = {
                // 图片
                'jpg': 'el-icon-picture',
                'jpeg': 'el-icon-picture',
                'png': 'el-icon-picture',
                'gif': 'el-icon-picture',
                'svg': 'el-icon-picture',
                'webp': 'el-icon-picture',

                // 文档
                'pdf': 'el-icon-document',
                'doc': 'el-icon-document',
                'docx': 'el-icon-document',
                'xls': 'el-icon-document',
                'xlsx': 'el-icon-document',
                'ppt': 'el-icon-document',
                'pptx': 'el-icon-document',

                // 文本
                'txt': 'el-icon-document',
                'md': 'el-icon-document',
                'json': 'el-icon-document',
                'xml': 'el-icon-document',
                'yaml': 'el-icon-document',
                'yml': 'el-icon-document',

                // 代码
                'js': 'el-icon-document',
                'java': 'el-icon-document',
                'py': 'el-icon-document',
                'go': 'el-icon-document',
                'cpp': 'el-icon-document',

                // 压缩包
                'zip': 'el-icon-box',
                'rar': 'el-icon-box',
                '7z': 'el-icon-box',
                'tar': 'el-icon-box',
                'gz': 'el-icon-box',

                // 视频
                'mp4': 'el-icon-video-camera',
                'avi': 'el-icon-video-camera',
                'mov': 'el-icon-video-camera',

                // 音频
                'mp3': 'el-icon-headset',
                'wav': 'el-icon-headset',
                'flac': 'el-icon-headset'
            };

            return iconMap[ext] || 'el-icon-document';
        };

        // 格式化文件大小
        const formatFileSize = (bytes) => {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        };

        // 格式化日期
        const formatDate = (date) => {
            if (!date) return '';
            try {
                return new Date(date).toLocaleString('zh-CN');
            } catch {
                return '';
            }
        };

        // 消息提示
        const showSuccess = (message) => {
            ElMessage.success(message);
        };

        const showError = (message) => {
            ElMessage.error(message);
        };

        // Element Plus Icons
        const {
            Search, Upload, Download, Delete, Refresh,
            FolderAdd, FolderOpened, UploadFilled, HomeFilled
        } = icons;

        return {
            // 数据
            buckets,
            currentBucket,
            currentPath,
            files,
            selectedFiles,
            loading,
            searchKeyword,
            uploadDialogVisible,
            folderDialogVisible,
            uploadFiles,
            uploading,
            folderName,
            dragOver,
            tableHeight,
            pagination,

            // 计算属性
            pathParts,
            sortedFiles,
            totalCount,

            // 方法
            onBucketChange,
            loadFiles,
            handlePageChange,
            handleFileClick,
            navigateToPath,
            getPathUpTo,
            handleSelectionChange,
            showUploadDialog,
            handleFileSelect,
            handleFileDrop,
            removeUploadFile,
            doUploadFiles,
            downloadFile,
            deleteFile,
            handleBatchDelete,
            showCreateFolderDialog,
            createFolder,
            handleSearch,
            getFileIcon,
            formatFileSize,
            formatDate,

            // Icons
            Search, Upload, Download, Delete, Refresh,
            FolderAdd, FolderOpened, UploadFilled, HomeFilled
        };
    }
});

// 注册所有图标
Object.keys(icons).forEach(key => {
    app.component(key, icons[key]);
});

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app');
