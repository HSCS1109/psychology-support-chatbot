<template>
    <div class="container">
        <div class="title">
            <div class="back-home">
                <el-icon><Back /></el-icon>
                <!--文案返回首页 -->
                <span>返回首页</span>
            </div>
            <div class="title-text">
                <h2>登录您的账户</h2>
                <p>请输入您的登录信息</p>
            </div>
        </div>
        <div class="form-container">
            <el-form
                ref="ruleFormRef"  
                :model="formData"
                :rules="rules"    
                label-position="top"
            >
                <el-form-item label="用户名或邮箱" prop="username">
                    <el-input v-model="formData.username" size="large" placeholder="请输入用户名"/>
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" size="large" placeholder="请输入密码" type="password" show-password />
                </el-form-item>
                <el-button class='btn' size="large" type="primary" @click="submitForm(ruleFormRef)">登录</el-button>
            </el-form>
            <div class="footer">
                <p>还没有账号？<router-link to="/auth/register">去注册</router-link></p>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { Back } from '@element-plus/icons-vue';
import {ref,reactive} from 'vue'
import {login} from '@/api/admin'
import { useRouter } from 'vue-router';

//   因为 el-form 组件的验证逻辑（ :rules ）是封装在组件内部的，外部无法直接访问。
// 通过 ref 获取组件实例后，才能调用它暴露出来的方法。
const ruleFormRef = ref()
const formData = reactive({
    username:'',
    password:''
})
const rules = reactive({
    username:[
        {required:true,message:'请输入用户名',trigger:'blur'}
    ],
    password:[
        {required:true,message:'请输入密码',trigger:'blur'}
    ]
})
// 登录
const router = useRouter()
const submitForm = async (formEl) => {
    if (!formEl) return
    // 触发表单验证
    await formEl.validate((valid,fields)=>{
        // 校验状态成功
        if (valid) {
            login(formData).then(data=>{
                // 判断token是否存在
                if(!data.token){
                    return console.error('登录失败')
                }
                // 登录成功,保存token和用户信息
                localStorage.setItem('token',data.token)
                // 将对象转换成json字符串
                localStorage.setItem('userInfo',JSON.stringify(data.userInfo))

                // 根据用户角色决定跳转的路径
                if(data.userInfo.userType === 2){
                    // 跳转到后台界面
                    router.push('/back/dashboard')
                } else {
                    // 跳转到用户界面
                    router.push('/')
                }
            })
        }
    })
}

</script>
<style scoped lang="scss">
.container{
    width: 384px;
    .title{
        .back-home {
            margin-bottom: 60px;
        }
        .title-text{
            text-align: center;
            h2{
                font-size: 36px;
                margin-bottom: 10px;
            }
            p {
                font-size: 18px;
                color:#6b7280;
            }
        }
    }
    .form-container{
        margin-top: 30px;
        .btn{
            margin-top:40px;
            width: 100%;
        }
        .footer{
            padding: 30px;
            text-align: center;
        }
    }
}
</style>