<template>
    <el-dialog
        :title="isEdit ? '编辑文章' : '新增文章'"
        v-model="dialogVisible"
        width="50%"
        @close="handleClose"
    >
        <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
            <el-form-item label="文章标题" prop="title">
                <el-input v-model="formData.title" placeholder="请输入文章标题" maxlength="200" show-word-limit clearable></el-input>
            </el-form-item>
            <el-form-item label="所属分类" prop="categoryId">
                <el-select v-model="formData.categoryId" placeholder="请选择分类">
                    <el-option v-for="item in props.categories" :key="item.value" :label="item.label" :value="item.value"/>
                </el-select>
            </el-form-item>
            <el-form-item label="文章摘要" prop="summary">
                <el-input type="textarea" v-model="formData.summary" placeholder="请输入文章摘要(可选)" maxlength="1000" show-word-limit :rows="4"></el-input>
            </el-form-item>
            <el-form-item label="标签" prop="tags">
                <el-select v-model="formData.tagArray" placeholder="请输入或选择标签" multiple filterable allow-create style="width: 100%;">
                    <el-option v-for="item in commonTages" :key="item" :label="item" :value="item"/>
                </el-select>
            </el-form-item>
            <el-form-item label="封面图片">
                <div class="cover-upload">
                    <el-upload
                        class="avatar-uploader"
                        action="#"
                        :before-upload="beforeUpload"
                        :http-request="handleUploadRequest"
                        :show-file-list="false"
                        accept="image/*"
                    >
                        <div v-if="!imgUrl" class="cover-placeholder">
                            <p>点击上传封面</p>
                        </div>
                        <img v-else :src="imgUrl" class="cover-image" alt="封面图片">
                    </el-upload>
                    <div v-if="imgUrl" class="cover-remove">
                        <el-button type="danger" size="small" @click="handleRemove">移除封面</el-button>
                    </div>
                </div>
            </el-form-item>
            <el-form-item label="文章内容" prop="content">
                <RichTextEditor 
                    v-model="formData.content" 
                    placeholder="请输入文章内容，支持富文本格式\n\n可以使用加粗、斜体、列表、标题等格式来丰富文章内容。"
                    :maxCharCount="5000"
                    @change="handleContentChange"
                    @created="handleEditorCreated"
                    min-height="400px"
                />
            </el-form-item>
        </el-form>
        <div v-if="btnPreview">
            <h3>内容预览</h3>
            <div v-html="formData.content"></div>
        </div>
        <!-- 使用具名插槽 -->
        <template #footer>
            <el-button @click="btnPreview = !btnPreview">{{ btnPreview ? "隐藏预览" : "预览效果" }}</el-button>
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="loading">{{ isEdit ? '更新文章' : '创建文章' }}</el-button>
        </template>
    </el-dialog>
</template>
<script setup>
import {ref,reactive, computed,watch,nextTick} from 'vue'
import {ElMessage} from 'element-plus'
import {uploadFile,createArticle,updateArticle} from '@/api/admin'
import {fileBaseUrl} from '@/config/index.js'
import RichTextEditor from '@/components/RichTextEditor.vue'
// 从外部传入弹窗打开和关闭的状态
// 子组件不能修改props，要在父组件中修改。
const props = defineProps({
    modelValue:{
        type:Boolean,
        default:false
    },
    categories:{
        type:Array,
        default:() => []
    },
    article:{
        type:Object,
        default:null
    }
})

const emit = defineEmits(['update:modelValue','success'])


// 用emit告诉父组件修改
const dialogVisible = computed({
    get(){
        return props.modelValue
    },
    set(val){
        // 父组件监听到子组件修改的值，就更新自己的值->弹窗关闭
        // 子组件不能直接修改父组件传过来的props，只能通过emit来修改
        emit('update:modelValue',val)
    }
})
// 判断当前是新增还是编辑文章
const isEdit = computed(()=>!!props.article?.id)
watch(()=>props.article,(newVal)=>{
    if (newVal){
            nextTick(()=>{
                Object.assign(formData,newVal)
            formData.categoryId = String(newVal.categoryId)
            // 使用现有的id
            businessId.value = newVal.id
            // 封面的url
            imgUrl.value = fileBaseUrl + newVal.coverImage
        })
       
    }
})
const handleClose = () => {
    // 重置表单
    formRef.value.resetFields()
    // 重置id
    businessId.value = null
    // 重置标签
    formData.tagArray = []
    // 重置封面图片和方法
    handleRemove()
    
    emit('update:modelValue',false)
    
}

// 表单数据
// 这些数据都是后端需要的参数，都需要提交给后端
const formData = reactive({
    "title": "",
    "content": "",
    "coverImage": "",
    "categoryId": "",
    "summary": "",
    "tags": "",
    "id": ""
})

const rules = reactive({
    title:[
        {required:true,message:'请输入文章标题',trigger:'blur'},
        {max:200,message:'文章标题最多200个字符',trigger:'blur'}
    ],
    categoryId:[
        {required:true,messaga:"请选择分类",trigger:"change"}
    ],
    content:[
        {required:true,message:'请输入文章内容',trigger:'blur'},
        {max:5000,message:'文章内容最多5000个字符',trigger:'blur'}
    ],
})

const commonTages = [
    '情绪管理','焦虑','抑郁','压力','睡眠',
    '冥想','正念','放松','心理健康','自我成长',
    '人际关系','工作压力','学习方法','生活技巧'
]

//上传图片
const imgUrl = ref('')
const beforeUpload = (file)=>{
    
    // 针对上传的文件进行校验
    const isImage =file.type.startsWith('image/')
    const isLt5M = file.size/1024/1024 < 5
    if (!isImage){
        ElMessage.error('请上传图片文件')
        return false
    }
    if (!isLt5M){
        ElMessage.error('图片文件大小不能超过5MB')
        return false
    }
    return true
}

const businessId = ref(null)
const handleUploadRequest = async ({ file })=>{
    // UUID生成业务id
    businessId.value = crypto.randomUUID()
    const fileRes = await uploadFile(file, {
        businessId:businessId.value
    })
    console.log(fileRes,'上传图片结果')

    // 拼接完整的图片地址
    imgUrl.value = fileBaseUrl + fileRes.filePath
    formData.coverImage = fileRes.filePath
}

const handleRemove = () => {
    imgUrl.value = ''
    formData.coverImage = ''
}

// 富文本
// 富文本输入的时候触发的函数
const handleContentChange = (data) => {
    // console.log(data,'富文本内容')
    formData.content = data.html
}

// 富文本创建完成后触发的函数.考虑新增和编辑的情况
// 考虑到富文本内容可能为空，所以要判断一下是否有内容
// 有内容，就设置富文本内容
// 富文本内容为空，就不设置富文本内容
const editorInstance = ref(null)
const handleEditorCreated = (editor) => {
    editorInstance.value = editor
    // 编辑的情况
    if(formData.content && editor){
        nextTick(() => {
            // 将内容重新设置给富文本编辑器
            editor.setHtml(formData.content)
        })
    }
}

const btnPreview = ref(false)

// 提交
const formRef = ref()
const loading = ref(false)
const handleSubmit = () => {
    formRef.value.validate((valid,fields)=>{
        if(valid){
            loading.value = true
            console.log(formData,'提交数据formdata')
            const submitData = {
                ...formData,
                tags:formData.tagArray.join(',')
            }
            delete submitData.tagArray
            if(!isEdit.value){
                submitData.id = businessId.value
                createArticle(submitData).then(res=>{
                loading.value = false
                ElMessage.success('文章创建成功')
                // 关闭弹窗
                emit('success')
                })
            } else {
                updateArticle(props.article.id, submitData).then(res=>{
                    loading.value = false
                    ElMessage.success('文章更新成功')
                    // 关闭弹窗
                    emit('success')
                })
            }
        }
    })
}


</script>

<style scoped lang="scss">
.cover-placeholder{
    width: 200px;
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    background-color: #f6f8fa;
}

.cover-image{
    width: 200px;
    height: 120px;
    display: block;
}
</style>