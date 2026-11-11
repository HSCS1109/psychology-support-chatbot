<template>
    <div class="container">
        <div class="title">
            <div class="title-text">
                <h2>创建您的账户</h2>
                <p>请填写注册信息</p>
            </div>
        </div>
        <div class="form-container">
            <el-form
            :model="formData"
            :rules="rules"
            ref="submitFormRef"
            label-position="top"
            >
                <el-form-item label="用户名或邮箱" prop="username">
                    <el-input v-model="formData.username" placeholder="请输入用户名" size="large"></el-input>
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="formData.email" placeholder="请输入邮箱" size="large"></el-input>
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="formData.nickname" placeholder="请输入昵称（可选）" size="large"></el-input>
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="formData.phone" placeholder="请输入手机号（可选）" size="large"></el-input>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" placeholder="请输入密码" size="large" type="password" show-password></el-input>
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="formData.confirmPassword" placeholder="请确认密码" size="large" type="password" show-password></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" size="large" class="btn" @click="submitForm(submitFormRef)">注册</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import {ref,reactive} from 'vue'
import {register} from '@/api/frontend'
import {ElMessage} from 'element-plus'
import {useRouter} from 'vue-router'

const router = useRouter()

const formData = reactive({
    "username": "",
    "email": "",
    "nickname": "",
    "phone": "",
    "password": "",
    "confirmPassword": "",
    "gender": 0,  // 性别 0: 男 1: 女
    "userType": 1  // 用户类型 1: 前台普通用户 2: 管理员
})

const rules = reactive({
    "username":[
        {required: true, message: '请输入用户名', trigger: 'blur'}
    ],
    "email":[
        {required: true, message: '请输入邮箱', trigger: 'blur'}
    ],
    "password":[
        {required: true, message: '请输入密码', trigger: 'blur'}
    ],
    "confirmPassword":[
        {required: true, message: '请确认密码', trigger: 'blur'}
    ]
})
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}
const submitFormRef = ref(null)
const submitForm = async (formEl) => {
    if(!formEl) return
    formEl.validate(async (valid) => {
        // 1. 检查表单验证是否通过，不通过则不提交
        if (!valid) return
        try {
            // 2. 拦截器已经解包了 response.data.data，直接接收即可
            const res = await register(formData)
            // 3. 走到这里说明 code === '200'，注册成功
            ElMessage.success('注册成功')
            router.push('/auth/login')
        } catch (err) {
            // 4. 失败时拦截器已显示错误消息，这里只记录日志
            console.error('注册失败:', err)
        }
    })
}

</script>

<style scoped lang="scss">
.container {
    width: 384px;
    .flex-box {
        display: flex;
        align-items: center;
    }
    .title {
        .title-text {
            text-align: center;
            h2 {
                font-size: 36px;
                margin-bottom: 10px;
            }
            p {
                font-size: 18px;
                color: #6b7280;
            }
        }
    }
    .form-container {
        margin-top: 30px;
        .btn {
            margin-top: 40px;
            width: 100%;
        }
        .footer {
            padding: 30px;
            text-align: center;
        }
    }
}
</style>