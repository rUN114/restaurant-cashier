<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.changelogTitle') }}</h3>
        <el-button v-if="canUpload" type="primary" @click="handleAdd">{{ $t('changelog.upload') }}</el-button>
      </div>

      <el-timeline v-if="list.length" v-loading="loading">
        <el-timeline-item
          v-for="item in list"
          :key="item.id"
          :timestamp="item.createdAt"
          placement="top"
          color="#409EFF"
          size="large"
          :type="item === list[0] ? 'primary' : 'info'"
        >
          <el-card shadow="hover">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span><el-tag type="primary">{{ item.version }}</el-tag> <strong>{{ item.title }}</strong></span>
                <span v-if="canUpload">
                  <el-button link type="primary" size="small" @click="handleEdit(item)">{{ $t('common.edit') }}</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(item.id)">{{ $t('common.delete') }}</el-button>
                </span>
              </div>
            </template>
            <div style="white-space:pre-wrap;line-height:1.8">{{ item.content }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :description="$t('common.noData')" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="editId ? '编辑更新日志' : $t('changelog.upload')" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('changelog.version')" required>
          <el-input v-model="form.version" placeholder="v2.1" />
        </el-form-item>
        <el-form-item :label="$t('changelog.title')" required>
          <el-input v-model="form.title" placeholder="新增XXX功能" />
        </el-form-item>
        <el-form-item :label="$t('changelog.content')" required>
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入更新内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getChangelogs, createChangelog, updateChangelog, deleteChangelog } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const user = useUserStore()
const canUpload = computed(() => ['ADMIN', 'STORE_ADMIN'].includes(user.roleCode))

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({ version: '', title: '', content: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getChangelogs()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  form.value = { version: '', title: '', content: '' }
  dialogVisible.value = true
}

const handleEdit = (item) => {
  editId.value = item.id
  form.value = { version: item.version, title: item.title, content: item.content }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.version || !form.value.title || !form.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (editId.value) {
      await updateChangelog(editId.value, form.value)
      ElMessage.success('编辑成功')
    } else {
      await createChangelog(form.value)
      ElMessage.success('上传成功')
    }
    dialogVisible.value = false
    load()
  } catch (_) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
    await deleteChangelog(id)
    ElMessage.success('删除成功')
    load()
  } catch (_) {}
}

onMounted(load)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
