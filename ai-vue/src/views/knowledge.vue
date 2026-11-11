<template>
    <div>
        <PageHead title="知识文章">
            <template v-slot:buttons>  
                <el-button type="primary" @click="handleEdit({})">新增</el-button>
            </template>
        </PageHead>
        <!-- 传入了方法，然后在组件中触发方法 -->
        <TableSearch :formItem="formItem" @search="handleSearch" />
        <el-table :data="tableData" style="width: 100%;margin-top:25px">
            <!-- 每一列 -->
            <el-table-column label="文章标题" fixed="left" width="150">
                <!-- 自定义，使用作用域插槽 -->
                <template #default="scope">
                    <div style="display: flex; align-items: center; ">
                        <el-icon><timer /></el-icon>
                        <span>{{ scope.row.title }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="分类" width="200">
                <template #default="scope">
                    <div style="display: flex;align-items: center;">
                        <el-icon><timer /></el-icon>
                        <span>{{ scope.row.categoryName }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="authorName" label="作者" width="150"/>
            <el-table-column prop="readCount" label="阅读量" width="150"/>
            <el-table-column prop="updatedAt" label="发布时间" width="150"/>
            <el-table-column label="操作" width="240" fixed="right">
                <template #default="scope">
                    <el-button text type="primary" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button @click="handlePublish(scope.row)" v-if="scope.row.status === 0 || scope.row.status === 2" text type="success">发布</el-button>
                    <el-button @click="handleUnpublish(scope.row)" v-if="scope.row.status === 1" text type="warning">下线</el-button>
                    <el-button @click="handleDelete(scope.row)" text type="danger">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            style="margin-top: 25px;"
            :page-size="pagination.size"
            layout="prev,pager,next" 
            :total="pagination.total"
            @change="handleChange"
        />
        <!-- v-model=传值+监听变化
            等价于
                :modelValue="dialogVisible",
                @update:modelValue="dialogVisible = $event"
        -->
        <ArticleDialogue v-model:modelValue="dialogVisible" :categories="categories" @success="handleSuccess" :article="currentArticle"/>
    </div>
</template>

<script setup>
import { onMounted,reactive,ref } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import {categoryTree,articlePage,getArticleDetail,changeArticleStatus,deleteArticle} from '@/api/admin'
import ArticleDialogue from '@/components/ArticleDialogue.vue'
import { ElMessage, ElMessageBox } from 'element-plus'


const formItem =[
    {comp:'input',prop:'title',label:'文章标题',placeholder:'请输入文章标题'},
    {comp:'select',prop:'categoryId',label:'文章分类',placeholder:'请选择文章分类'},
    {comp:'select',prop:'status', label:'状态',placeholder:'请选择状态',options:[
        {label:'草稿',value:0},
        {label:'已发布',value:1},
        {label:'已下线',value:2}
    ]}
]
// 分页参数
const pagination = reactive({
    currentPage:1,
    size:10,
    total:0
})
// 进行回调用的，组件内部查询按钮，点击后内部会搜集form表单数据集，对外进行暴露。在组件内部调用，然后拿到form表单结果。
// 获取列表数据.articlePage这个接口应用的地方:点击查询按钮/分页切换/页面首次加载时
const handleSearch = async (formData) => {
    const params = {
        ...formData,
        ...pagination,
    }
    // 解构数据
    const {records,total} = await articlePage(params)
    // console.log(records,total,'数据xxxxxxxxxxxxxxxxxxxx')
    tableData.value = records
    pagination.total = total

}
const handleChange = (page) => {
    // 将点击的页数赋值给当前页码，然后再执行搜索操作
    pagination.currentPage = page
    handleSearch()
}
// 分类映射
const categoryMap = reactive({})

// 分类列表
const categories = ref([])

// 列表数据
const tableData = ref([])

// 新增和编辑
const dialogVisible = ref(false)
const handleSuccess = ()=>{
    // 关闭弹窗
    dialogVisible.value = false
    // 刷新列表
    handleSearch()
}

const currentArticle = ref(null)
const handleEdit = (row)=>{
    if (!row.id) {
        // 新增
        currentArticle.value = null
        dialogVisible.value = true
    } else {
        // 编辑
        getArticleDetail(row.id).then(res=>{
            console.log(res,'文章详情')
            currentArticle.value = res
            dialogVisible.value = true
        })
    }
}
// 发布
const handlePublish = (row) => {
    ElMessageBox.confirm(
        `确认发布${row.title}吗？`,
        '确认',
        {
            confirmButtonText:'确定发布',
            cancelButtonText:'取消',
            type:'info'
        }
    ).then(()=>{
        changeArticleStatus(row.id,{status:1}).then(res=>{
            ElMessage.success('发布成功')
            handleSearch()
        })
    })
}

// 下线
const handleUnpublish = (row) => {
    ElMessageBox.confirm(
        `确认下线${row.title}吗？`,
        '确认',
        {
            confirmButtonText:'确定下线',
            cancelButtonText:'取消',
            type:'warning'
        }
    ).then(()=>{
        changeArticleStatus(row.id,{status:2}).then(res=>{
            ElMessage.success('下线成功')
            handleSearch()
        })
    })
}

// 删除
const handleDelete = (row) => {
    ElMessageBox.confirm(
        `确认删除${row.title}吗？`,
        '确认',
        {
            confirmButtonText:'确定删除',
            cancelButtonText:'取消',
            type:'danger'
        }
    ).then(()=>{
        deleteArticle(row.id).then(res=>{
            ElMessage.success('删除成功')
            handleSearch()
        })
    })
}

onMounted(async () => {
    const data = await categoryTree()

    categories.value = data.map(item => {
        categoryMap[item.id] = item.categoryName
        return{
            label:item.categoryName,
            value:String(item.id)
        }
    })
    formItem[1].options = categories.value
    // 获取列表
    handleSearch()
})
</script>