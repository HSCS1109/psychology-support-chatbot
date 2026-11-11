<template>
    <!-- formData为响应式数据，用于存储表单数据集 -->
    <el-form ref="ruleFormRef" :model="formData">
        <!-- el-row默认是24栅格 -->
        <el-row :gutter="24">
            <template v-for="item in formItemAttrs" :key="item.prop">
                <el-col v-bind="item.col">
                    <el-form-item :label="item.label" :prop="item.prop">
                        <!-- select组件下面要有option才会有内容 -->
                         <!-- 组件内部也要双向绑定 -->
                        <component v-model="formData[item.prop]" :is="isComp(item.comp)" :placeholder="item.placeholder">
                            <template v-if="item.comp === 'select'">
                                <!-- 写死全部选项 -->
                                <el-option label="全部" value=""/>
                                <el-option 
                                    v-for="opt in item.options"
                                    :key="opt.value"
                                    :label="opt.label"
                                    :value="opt.value"/>
                            </template>
                        </component>
                    </el-form-item>
                </el-col>
                
            </template>
        </el-row>
        
        <el-row>
            <!-- 查询的数据就是formData对象 -->
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset(ruleFormRef)">重置</el-button>
        </el-row>
    </el-form>
</template>

<script setup>
import {ref,reactive,computed} from 'vue'

// 父组件传的数据
const props = defineProps({
    formItem:{
        type:Array,
        default:()=>[]
    }
})
// 子组件声明可以发送的事件，触发这个事件传递两个参数，一个是事件名，一个是传递的数据
const emit = defineEmits(['search'])
// 给传入进来的formItem添加不同屏幕尺寸下span的值.
const formItemAttrs = computed(()=>{
    const { formItem } = props
    formItem.forEach(item=>{
        // 栅格数默认是24，根据屏幕尺寸动态调整
        item.col = { xs:24,sm:12,md:8,lg:6,xl:6 }
    })
    // computed要returnformItem，否则会报错
    return formItem
})

// 表单数据
const ruleFormRef = ref()
// 初始的表单数据，里面的属性是根据当前的prop决定的
const formData = reactive({})
// 用comp作为key去找对应的value
const isComp = (comp) => {
    return{
       input:'elInput',
       select:'elSelect'
    }[comp]
}
// 查询
// 子组件触发事件并把数据传递给父组件
const handleSearch = ()=>{
    // console.log(formData,'查询参数')
    emit('search',formData)
}
// 重置

const handleReset = (formEl)=>{
    // 先重置表单，再触发查询
    if(!formEl) return
    formEl.resetFields()
    emit('search',formData)

}
</script>

<style scoped>

</style>
