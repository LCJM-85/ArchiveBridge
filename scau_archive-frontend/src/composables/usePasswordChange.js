import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/modules/auth'

export function usePasswordChange(onSuccess) {
  const passwordDialogVisible = ref(false)
  const passwordFormRef = ref(null)
  const loading = ref(false)
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  const validateConfirmPassword = (rule, value, callback) => {
    if (value !== passwordForm.newPassword) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const passwordRules = {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }

  function showPasswordDialog() {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
  }

  async function handleChangePassword() {
    const valid = await passwordFormRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
      const res = await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      if (res.data.success) {
        ElMessage.success('密码修改成功，请重新登录')
        passwordDialogVisible.value = false
        onSuccess?.()
      } else {
        ElMessage.error(res.data.message || '修改失败')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '修改失败')
    } finally {
      loading.value = false
    }
  }

  return {
    passwordDialogVisible,
    passwordFormRef,
    loading,
    passwordForm,
    passwordRules,
    showPasswordDialog,
    handleChangePassword
  }
}
