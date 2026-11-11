import service from "@/utils/request";
import { selectProps } from "element-plus";
// serviece为axios的实例，可以调用get、post、delete、put这样的请求
export function login(data){
    // 接收两个参数，第一个参数是请求的路径，第二个参数是data
    return service.post('/user/login',data)
}

export function categoryTree(){
    // 接收两个参数，第一个参数是请求的路径，第二个参数是data
    return service.get('/knowledge/category/tree')
}

export function articlePage(params){
    return service.get('/knowledge/article/page',{params:params})
}

export function uploadFile(file,businessInfo){
    // 创建formdata对象
    const formData = new FormData()
    formData.append('file',file)
    formData.append('businessType','ARTICLE')
    formData.append('businessId',businessInfo.businessId)
    formData.append('businessField','cover')

    return service.post('/file/upload',formData,{
        // 文件类型为multipart/form-data
        headers:{
            'content-type':'multipart/form-data'
        }
    })
}

export function createArticle(data){
    return service.post('/knowledge/article',data)
}

export function getArticleDetail(id){
    return service.get(`/knowledge/article/${id}`)
}

export function updateArticle(id,data){
    return service.put(`/knowledge/article/${id}`,data)
}

export function changeArticleStatus(id,data){
    return service.put(`/knowledge/article/${id}/status`,data)
}  

export function deleteArticle(id){
    return service.delete(`/knowledge/article/${id}`)
}

export function getConsulationPage(params){
    return service.get('/psychological-chat/sessions',{params:params})
}

export function getSessionDetail(sessionId){
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export function getEmotionalPage(params){
    return service.get('/emotion-diary/admin/page',{params:params})
}

export function deleteEmotional(id){
    return service.delete(`/emotion-diary/admin/${id}`)
}

export function getAnalyticsOverview(){
    return service.get('/data-analytics/overview')
}

export function logout(){
    return service.post('/user/logout')
}