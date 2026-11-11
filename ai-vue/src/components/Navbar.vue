<template>
    <div class="navbar">
        <div class="flex-box">
            <el-button @click="handleCollapse">
                <el-icon><Expand /></el-icon>
            </el-button>
            <p class="page-title">{{ route.meta.title }}</p>
        </div>
        <div class="flex-box">
            <el-dropdown @command="handleCommand">
                <div class="flex-box">
                    <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                    <p class="user-name">admin</p>
                    <el-icon><ArrowDown /></el-icon>
                </div>
                <template v-slot:dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="logout">
                            退出登录
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Expand } from '@element-plus/icons-vue';
import { useAdminStore } from '@/stores/admin';
import {useRouter,useRoute} from'vue-router'
import { ElMessageBox } from 'element-plus';
import { logout } from '@/api/admin';

const router = useRouter()
const route = useRoute()

const handleCommand = (command) => {
    if (command === 'logout') {
        // 处理退出登录逻辑
        ElMessageBox.confirm('确定退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            // 无论后端logout是否成功，都清除本地登录态
            logout().catch(()=>{}).finally(() => {
                // 清除缓存
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
                // 跳转到登录页
                router.push('/auth/login')
            })
        }
    )}
}

const handleCollapse = () => {
    useAdminStore().toggleCollapse()    
}
</script>

<style scoped>
.navbar {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 15px;
    background: white;
    box-shadow: 0 1px 4px rgb(0,21,41,0.08);
    border-bottom: 1px solid #e5e7eb;
    .flex-box {
        display: flex;
        align-items: center;
        justify-content: center;
    }
    .page-title {
        margin-left: 20px;
        font-size: 26px;
        font-weight: bold;
        color: #1f2937;
    }
}
</style>

